package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.exception.CannotChangeOwnAccountException;
import br.com.devready.application.identity.exception.NotAnActiveAdminException;
import br.com.devready.application.identity.exception.UserNotFoundException;
import br.com.devready.application.identity.fake.FakePasswordGenerator;
import br.com.devready.application.identity.fake.FakePasswordHasher;
import br.com.devready.application.identity.fake.InMemoryRefreshTokenStore;
import br.com.devready.application.identity.fake.InMemoryUserRepository;
import br.com.devready.application.shared.DirectTransactions;
import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.SocialAccount;
import br.com.devready.domain.identity.SocialProvider;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.identity.WeakPasswordException;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResetUserPasswordUseCaseTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryRefreshTokenStore refreshTokens = new InMemoryRefreshTokenStore();
    private final FakePasswordHasher hasher = new FakePasswordHasher();
    private final FakePasswordGenerator passwords = new FakePasswordGenerator();
    private final DirectTransactions transactions = new DirectTransactions();
    private final ResetUserPasswordUseCase reset =
            new ResetUserPasswordUseCase(users, hasher, passwords, refreshTokens, transactions);

    private final User actor = admin(1, "Admin");

    private User candidate(int n, String name) {
        return users.save(
                User.register(
                        new UUID(0, n),
                        name.toLowerCase(Locale.ROOT) + "@example.com",
                        name,
                        hasher.hash("senha-antiga"),
                        T0.plusSeconds(n)));
    }

    private User admin(int n, String name) {
        User user = candidate(n, name);
        user.promoteToAdmin();
        return users.save(user);
    }

    private User stored(User user) {
        return users.findById(user.id()).orElseThrow();
    }

    @Test
    void givesATemporaryPasswordAndEndsTheSessionsOfThatAccountOnly() {
        User ana = candidate(2, "Ana");
        User bia = candidate(3, "Bia");
        String anaSession = refreshTokens.issue(ana.id());
        String biaSession = refreshTokens.issue(bia.id());
        String actorSession = refreshTokens.issue(actor.id());
        passwords.willGenerate("Provisoria123456");
        users.resetSaveCount();

        String temporary = reset.execute(actor.id(), ana.id());

        assertThat(temporary).isEqualTo("Provisoria123456");
        assertThat(stored(ana).passwordHash()).isEqualTo("hashed:Provisoria123456");
        assertThat(stored(ana).mustChangePassword()).isTrue();
        assertThat(refreshTokens.isActive(anaSession)).isFalse();
        assertThat(refreshTokens.isActive(biaSession)).isTrue();
        assertThat(refreshTokens.isActive(actorSession)).isTrue();
        assertThat(stored(bia).mustChangePassword()).isFalse();
        assertThat(users.saveCount()).isEqualTo(1);
        assertThat(passwords.calls()).isEqualTo(1);
        assertThat(transactions.calls()).isEqualTo(1);
        assertThat(users.lockCalls()).isEqualTo(1);
    }

    @Test
    void anAccountWithoutPasswordGetsOne() {
        User bob =
                users.save(
                        User.fromSocial(
                                new UUID(0, 2),
                                "bob@example.com",
                                "Bob",
                                null,
                                new SocialAccount(SocialProvider.GOOGLE, "g-bob"),
                                T0));

        String temporary = reset.execute(actor.id(), bob.id());

        assertThat(stored(bob).hasPassword()).isTrue();
        assertThat(hasher.matches(temporary, stored(bob).passwordHash())).isTrue();
        assertThat(stored(bob).mustChangePassword()).isTrue();
    }

    @Test
    void anotherAdminCanBeReset() {
        User bia = admin(2, "Bia");

        reset.execute(actor.id(), bia.id());

        assertThat(stored(bia).mustChangePassword()).isTrue();
        assertThat(stored(bia).isActiveAdmin()).isTrue();
    }

    @Test
    void aGeneratedPasswordOutsideThePolicyIsNeverStored() {
        User ana = candidate(2, "Ana");
        String session = refreshTokens.issue(ana.id());
        passwords.willGenerate("curta");
        users.resetSaveCount();

        assertThatThrownBy(() -> reset.execute(actor.id(), ana.id()))
                .isInstanceOf(WeakPasswordException.class);

        assertThat(stored(ana).passwordHash()).isEqualTo("hashed:senha-antiga");
        assertThat(stored(ana).mustChangePassword()).isFalse();
        assertThat(refreshTokens.isActive(session)).isTrue();
        assertThat(users.saveCount()).isZero();
    }

    @Test
    void nobodyResetsTheirOwnPasswordHere() {
        String session = refreshTokens.issue(actor.id());
        users.resetSaveCount();

        assertThatThrownBy(() -> reset.execute(actor.id(), actor.id()))
                .isInstanceOf(CannotChangeOwnAccountException.class)
                .hasMessage("Você não pode alterar a sua própria conta por aqui.");

        assertThat(stored(actor).mustChangePassword()).isFalse();
        assertThat(refreshTokens.isActive(session)).isTrue();
        assertThat(users.saveCount()).isZero();
        assertThat(passwords.calls()).isZero();
        assertThat(transactions.calls()).isZero();
    }

    @Test
    void anUnknownAccountIsNotFound() {
        assertThatThrownBy(() -> reset.execute(actor.id(), new UUID(9, 9)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuário não encontrado.");
        assertThat(passwords.calls()).isZero();
    }

    @Test
    void anAdminDemotedAfterSigningInIsRefusedAfterTheLock() {
        User ana = candidate(2, "Ana");
        String session = refreshTokens.issue(ana.id());
        actor.demoteToCandidate();
        users.save(actor);
        users.resetSaveCount();

        assertThatThrownBy(() -> reset.execute(actor.id(), ana.id()))
                .isInstanceOf(NotAnActiveAdminException.class)
                .hasMessage("Você não tem mais acesso à gestão de usuários.");
        assertThatThrownBy(() -> reset.execute(actor.id(), new UUID(9, 9)))
                .isInstanceOf(NotAnActiveAdminException.class);

        assertThat(stored(ana).passwordHash()).isEqualTo("hashed:senha-antiga");
        assertThat(refreshTokens.isActive(session)).isTrue();
        assertThat(users.saveCount()).isZero();
        assertThat(transactions.calls()).isEqualTo(2);
        assertThat(users.lockCalls()).isEqualTo(2);
    }

    @Test
    void anAdminDeactivatedAfterSigningInIsRefused() {
        User ana = candidate(2, "Ana");
        actor.deactivate();
        users.save(actor);

        assertThatThrownBy(() -> reset.execute(actor.id(), ana.id()))
                .isInstanceOf(NotAnActiveAdminException.class);
        assertThat(stored(ana).mustChangePassword()).isFalse();
    }

    @Test
    void anAdminWithAPendingTemporaryPasswordIsRefused() {
        User bia = admin(2, "Bia");
        String session = refreshTokens.issue(bia.id());
        actor.resetPassword(hasher.hash("Provisoria654321"));
        users.save(actor);
        users.resetSaveCount();

        assertThatThrownBy(() -> reset.execute(actor.id(), bia.id()))
                .isInstanceOf(NotAnActiveAdminException.class)
                .hasMessage("Você não tem mais acesso à gestão de usuários.");

        assertThat(stored(bia).mustChangePassword()).isFalse();
        assertThat(refreshTokens.isActive(session)).isTrue();
        assertThat(users.saveCount()).isZero();
        assertThat(passwords.calls()).isZero();
        assertThat(users.lockCalls()).isEqualTo(1);
    }

    @Test
    void anActorThatDoesNotExistIsRefused() {
        User ana = candidate(2, "Ana");

        assertThatThrownBy(() -> reset.execute(new UUID(8, 8), ana.id()))
                .isInstanceOf(NotAnActiveAdminException.class);
        assertThat(stored(ana).mustChangePassword()).isFalse();
    }

    @Test
    void whenTheSessionsCannotBeEndedNothingIsStored() {
        User ana = candidate(2, "Ana");
        InMemoryRefreshTokenStore failing =
                new InMemoryRefreshTokenStore() {
                    @Override
                    public void revokeAll(UUID userId) {
                        throw new IllegalStateException("Redis fora do ar");
                    }
                };
        ResetUserPasswordUseCase resetWithoutRedis =
                new ResetUserPasswordUseCase(users, hasher, passwords, failing, transactions);
        users.resetSaveCount();

        assertThatThrownBy(() -> resetWithoutRedis.execute(actor.id(), ana.id()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(stored(ana).passwordHash()).isEqualTo("hashed:senha-antiga");
        assertThat(stored(ana).mustChangePassword()).isFalse();
        assertThat(users.saveCount()).isZero();
    }

    @Test
    void whatWasWrittenAfterTheReadIsNotUndone() {
        UUID anaId = new UUID(0, 2);
        InMemoryUserRepository racing =
                new InMemoryUserRepository() {
                    @Override
                    public Optional<User> findById(UUID id) {
                        Optional<User> stored = super.findById(id);
                        if (!id.equals(anaId)) {
                            return stored;
                        }
                        Optional<User> read = stored.map(ResetUserPasswordUseCaseTest::copy);
                        stored.ifPresent(
                                user -> {
                                    user.updateProfile("Ana Souza", null);
                                    user.promoteToAdmin();
                                });
                        return read;
                    }
                };
        User admin = User.register(new UUID(0, 1), "admin@example.com", "Admin", "hash", T0);
        admin.promoteToAdmin();
        racing.save(admin);
        racing.save(User.register(anaId, "ana@example.com", "Ana", "hash", T0.plusSeconds(2)));
        ResetUserPasswordUseCase resetWithRace =
                new ResetUserPasswordUseCase(
                        racing, hasher, passwords, refreshTokens, transactions);

        String temporary = resetWithRace.execute(admin.id(), anaId);

        User stored = racing.all().get(1);
        assertThat(stored.displayName()).isEqualTo("Ana Souza");
        assertThat(stored.role()).isEqualTo(Role.ADMIN);
        assertThat(stored.passwordHash()).isEqualTo(hasher.hash(temporary));
        assertThat(stored.mustChangePassword()).isTrue();
    }

    private static User copy(User user) {
        return User.restore(
                user.id(),
                user.email(),
                user.displayName(),
                user.avatarUrl(),
                user.passwordHash(),
                user.role(),
                user.active(),
                user.mustChangePassword(),
                user.createdAt(),
                user.socialAccounts());
    }
}
