package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.dto.ChangeUserAccessCommand;
import br.com.devready.application.identity.exception.CannotChangeOwnAccountException;
import br.com.devready.application.identity.exception.LastActiveAdminException;
import br.com.devready.application.identity.exception.NotAnActiveAdminException;
import br.com.devready.application.identity.exception.NothingToChangeException;
import br.com.devready.application.identity.exception.UserNotFoundException;
import br.com.devready.application.identity.fake.InMemoryRefreshTokenStore;
import br.com.devready.application.identity.fake.InMemoryUserRepository;
import br.com.devready.application.shared.DirectTransactions;
import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.User;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChangeUserAccessUseCaseTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryRefreshTokenStore refreshTokens = new InMemoryRefreshTokenStore();
    private final DirectTransactions transactions = new DirectTransactions();
    private final ChangeUserAccessUseCase change =
            new ChangeUserAccessUseCase(users, refreshTokens, transactions);

    private final User actor = admin(1, "Admin");

    private User candidate(int n, String name) {
        return users.save(
                User.register(
                        new UUID(0, n),
                        name.toLowerCase(Locale.ROOT) + "@example.com",
                        name,
                        "hash",
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

    private static ChangeUserAccessCommand active(boolean value) {
        return new ChangeUserAccessCommand(value, null);
    }

    private static ChangeUserAccessCommand role(Role value) {
        return new ChangeUserAccessCommand(null, value);
    }

    private void actorLeftOutOfTheCount() {
        users.leaveOutOfAdminCount(actor.id());
    }

    @Test
    void deactivatingEndsTheSessionsOfThatAccountOnly() {
        User ana = candidate(2, "Ana");
        User bia = candidate(3, "Bia");
        String anaSession = refreshTokens.issue(ana.id());
        String biaSession = refreshTokens.issue(bia.id());
        users.resetSaveCount();

        User changed = change.execute(actor.id(), ana.id(), active(false));

        assertThat(changed.active()).isFalse();
        assertThat(stored(ana).active()).isFalse();
        assertThat(refreshTokens.isActive(anaSession)).isFalse();
        assertThat(refreshTokens.isActive(biaSession)).isTrue();
        assertThat(users.saveCount()).isEqualTo(1);
        assertThat(transactions.calls()).isEqualTo(1);
        assertThat(users.lockCalls()).isEqualTo(1);
    }

    @Test
    void reactivatingSavesWithoutRevokingAnything() {
        User ana = candidate(2, "Ana");
        ana.deactivate();
        users.save(ana);
        String session = refreshTokens.issue(ana.id());
        users.resetSaveCount();

        User changed = change.execute(actor.id(), ana.id(), active(true));

        assertThat(changed.active()).isTrue();
        assertThat(stored(ana).active()).isTrue();
        assertThat(users.saveCount()).isEqualTo(1);
        assertThat(refreshTokens.isActive(session)).isTrue();
    }

    @Test
    void theValueThatIsAlreadyThereChangesNothing() {
        User ana = candidate(2, "Ana");
        ana.deactivate();
        users.save(ana);
        String session = refreshTokens.issue(ana.id());
        users.resetSaveCount();

        assertThat(change.execute(actor.id(), ana.id(), active(false)).active()).isFalse();
        assertThat(change.execute(actor.id(), ana.id(), role(Role.CANDIDATE)).role())
                .isEqualTo(Role.CANDIDATE);
        assertThat(
                        change.execute(
                                        actor.id(),
                                        ana.id(),
                                        new ChangeUserAccessCommand(false, Role.CANDIDATE))
                                .active())
                .isFalse();

        assertThat(users.saveCount()).isZero();
        assertThat(refreshTokens.isActive(session)).isTrue();
    }

    @Test
    void promotesAndDemotesWithoutEndingSessions() {
        User ana = candidate(2, "Ana");
        String session = refreshTokens.issue(ana.id());

        assertThat(change.execute(actor.id(), ana.id(), role(Role.ADMIN)).isAdmin()).isTrue();
        assertThat(stored(ana).role()).isEqualTo(Role.ADMIN);
        assertThat(change.execute(actor.id(), ana.id(), role(Role.CANDIDATE)).isAdmin()).isFalse();
        assertThat(stored(ana).role()).isEqualTo(Role.CANDIDATE);
        assertThat(refreshTokens.isActive(session)).isTrue();
    }

    @Test
    void changesBothFieldsAtOnceAndRevokesOnce() {
        User bia = admin(2, "Bia");
        String session = refreshTokens.issue(bia.id());
        users.resetSaveCount();

        User changed =
                change.execute(
                        actor.id(), bia.id(), new ChangeUserAccessCommand(false, Role.CANDIDATE));

        assertThat(changed.active()).isFalse();
        assertThat(changed.role()).isEqualTo(Role.CANDIDATE);
        assertThat(refreshTokens.isActive(session)).isFalse();
        assertThat(users.saveCount()).isEqualTo(1);
    }

    @Test
    void nobodyChangesTheirOwnAccountHere() {
        users.resetSaveCount();

        for (ChangeUserAccessCommand command :
                List.of(active(false), role(Role.CANDIDATE), active(true))) {
            assertThatThrownBy(() -> change.execute(actor.id(), actor.id(), command))
                    .isInstanceOf(CannotChangeOwnAccountException.class)
                    .hasMessage("Você não pode alterar a sua própria conta por aqui.");
        }

        assertThat(stored(actor).isActiveAdmin()).isTrue();
        assertThat(users.saveCount()).isZero();
        assertThat(transactions.calls()).isZero();
    }

    @Test
    void anEmptyRequestIsRefusedBeforeAnything() {
        User ana = candidate(2, "Ana");

        assertThatThrownBy(
                        () ->
                                change.execute(
                                        actor.id(),
                                        ana.id(),
                                        new ChangeUserAccessCommand(null, null)))
                .isInstanceOf(NothingToChangeException.class)
                .hasMessage("Informe o que mudar: a situação da conta, o papel ou os dois.");
        assertThat(transactions.calls()).isZero();
    }

    @Test
    void anUnknownAccountIsNotFound() {
        assertThatThrownBy(() -> change.execute(actor.id(), new UUID(9, 9), active(false)))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("Usuário não encontrado.");
    }

    @Test
    void theLastActiveAdminCannotBeDemotedNorDeactivated() {
        User bia = admin(2, "Bia");
        String session = refreshTokens.issue(bia.id());
        actorLeftOutOfTheCount();
        users.resetSaveCount();

        for (ChangeUserAccessCommand command :
                List.of(
                        role(Role.CANDIDATE),
                        active(false),
                        new ChangeUserAccessCommand(false, Role.CANDIDATE))) {
            assertThatThrownBy(() -> change.execute(actor.id(), bia.id(), command))
                    .isInstanceOf(LastActiveAdminException.class)
                    .hasMessage("O sistema precisa de ao menos um admin ativo.");
        }

        assertThat(stored(bia).isActiveAdmin()).isTrue();
        assertThat(users.saveCount()).isZero();
        assertThat(refreshTokens.isActive(session)).isTrue();
    }

    @Test
    void anInactiveAdminDoesNotCountAsActive() {
        User bia = admin(2, "Bia");
        User caio = admin(3, "Caio");
        caio.deactivate();
        users.save(caio);
        actorLeftOutOfTheCount();

        assertThatThrownBy(() -> change.execute(actor.id(), bia.id(), role(Role.CANDIDATE)))
                .isInstanceOf(LastActiveAdminException.class);
        assertThat(change.execute(actor.id(), caio.id(), role(Role.CANDIDATE)).role())
                .isEqualTo(Role.CANDIDATE);
    }

    @Test
    void withAnotherActiveAdminTheChangeGoesThrough() {
        User bia = admin(2, "Bia");

        assertThat(change.execute(actor.id(), bia.id(), active(false)).active()).isFalse();
        assertThat(change.execute(actor.id(), bia.id(), role(Role.CANDIDATE)).role())
                .isEqualTo(Role.CANDIDATE);
    }

    @Test
    void anAdminDemotedAfterSigningInIsRefusedAfterTheLock() {
        User ana = candidate(2, "Ana");
        actor.demoteToCandidate();
        users.save(actor);
        users.resetSaveCount();

        assertThatThrownBy(() -> change.execute(actor.id(), ana.id(), role(Role.ADMIN)))
                .isInstanceOf(NotAnActiveAdminException.class)
                .hasMessage("Você não tem mais acesso à gestão de usuários.");
        assertThatThrownBy(() -> change.execute(actor.id(), new UUID(9, 9), active(false)))
                .isInstanceOf(NotAnActiveAdminException.class);

        assertThat(stored(ana).role()).isEqualTo(Role.CANDIDATE);
        assertThat(users.saveCount()).isZero();
        assertThat(transactions.calls()).isEqualTo(2);
        assertThat(users.lockCalls()).isEqualTo(2);
    }

    @Test
    void anAdminDeactivatedAfterSigningInIsRefused() {
        User ana = candidate(2, "Ana");
        String session = refreshTokens.issue(ana.id());
        actor.deactivate();
        users.save(actor);
        users.resetSaveCount();

        assertThatThrownBy(() -> change.execute(actor.id(), ana.id(), active(false)))
                .isInstanceOf(NotAnActiveAdminException.class)
                .hasMessage("Você não tem mais acesso à gestão de usuários.");

        assertThat(stored(ana).active()).isTrue();
        assertThat(refreshTokens.isActive(session)).isTrue();
        assertThat(users.saveCount()).isZero();
    }

    @Test
    void anAdminWithAPendingTemporaryPasswordIsRefused() {
        User ana = candidate(2, "Ana");
        actor.resetPassword("hash-provisorio");
        users.save(actor);
        users.resetSaveCount();

        assertThatThrownBy(() -> change.execute(actor.id(), ana.id(), role(Role.ADMIN)))
                .isInstanceOf(NotAnActiveAdminException.class)
                .hasMessage("Você não tem mais acesso à gestão de usuários.");

        assertThat(stored(ana).role()).isEqualTo(Role.CANDIDATE);
        assertThat(users.saveCount()).isZero();
        assertThat(users.lockCalls()).isEqualTo(1);
    }

    @Test
    void anActorThatDoesNotExistIsRefused() {
        User ana = candidate(2, "Ana");

        assertThatThrownBy(() -> change.execute(new UUID(8, 8), ana.id(), active(false)))
                .isInstanceOf(NotAnActiveAdminException.class);
        assertThat(stored(ana).active()).isTrue();
    }

    @Test
    void aProfileChangeWrittenAfterTheReadIsNotUndone() {
        UUID anaId = new UUID(0, 2);
        InMemoryUserRepository racing =
                new InMemoryUserRepository() {
                    @Override
                    public Optional<User> findById(UUID id) {
                        Optional<User> stored = super.findById(id);
                        if (!id.equals(anaId)) {
                            return stored;
                        }
                        Optional<User> read = stored.map(ChangeUserAccessUseCaseTest::copy);
                        stored.ifPresent(
                                user -> {
                                    user.updateProfile("Ana Souza", null);
                                    user.changePassword("hash-novo");
                                });
                        return read;
                    }
                };
        User admin = User.register(new UUID(0, 1), "admin@example.com", "Admin", "hash", T0);
        admin.promoteToAdmin();
        racing.save(admin);
        racing.save(User.register(anaId, "ana@example.com", "Ana", "hash", T0.plusSeconds(2)));
        ChangeUserAccessUseCase changeWithRace =
                new ChangeUserAccessUseCase(racing, refreshTokens, transactions);

        User answered = changeWithRace.execute(admin.id(), anaId, active(false));

        User stored = racing.all().get(1);
        assertThat(stored.active()).isFalse();
        assertThat(stored.displayName()).isEqualTo("Ana Souza");
        assertThat(stored.passwordHash()).isEqualTo("hash-novo");
        assertThat(answered.active()).isFalse();
        assertThat(answered.passwordHash()).isEqualTo("hash-novo");
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
