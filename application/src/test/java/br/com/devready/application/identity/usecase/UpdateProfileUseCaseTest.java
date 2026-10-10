package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.dto.UpdateProfileCommand;
import br.com.devready.application.identity.exception.InvalidCredentialsException;
import br.com.devready.application.identity.exception.InvalidCurrentPasswordException;
import br.com.devready.application.identity.exception.NewPasswordSameAsTemporaryException;
import br.com.devready.application.identity.exception.PasswordChangedMeanwhileException;
import br.com.devready.application.identity.exception.UserNotFoundException;
import br.com.devready.application.identity.fake.IdentityFixtures;
import br.com.devready.application.identity.fake.InMemoryUserRepository;
import br.com.devready.domain.identity.SocialAccount;
import br.com.devready.domain.identity.SocialProvider;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.identity.WeakPasswordException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UpdateProfileUseCaseTest {

    private final IdentityFixtures f = new IdentityFixtures();
    private final UpdateProfileUseCase update =
            new UpdateProfileUseCase(f.users, f.hasher, f.refreshTokens, f.transactions);
    private final GetCurrentUserUseCase current = new GetCurrentUserUseCase(f.users);

    private User ana() {
        return f.users.save(
                User.register(
                        f.ids.next(),
                        "ana@example.com",
                        "Ana",
                        f.hasher.hash("senha-atual"),
                        f.clock.now()));
    }

    private User socialBob() {
        return f.users.save(
                User.fromSocial(
                        f.ids.next(),
                        "bob@example.com",
                        "Bob",
                        null,
                        new SocialAccount(SocialProvider.GOOGLE, "g-bob"),
                        f.clock.now()));
    }

    @Test
    void updatesNameAndAvatarWithoutTouchingPasswordOrSessions() {
        User ana = ana();
        String refresh = f.refreshTokens.issue(ana.id());

        update.execute(
                new UpdateProfileCommand(ana.id(), "Ana Souza", "https://img/x.png", null, null));

        User reloaded = current.execute(ana.id());
        assertThat(reloaded.displayName()).isEqualTo("Ana Souza");
        assertThat(reloaded.avatarUrl()).isEqualTo("https://img/x.png");
        assertThat(reloaded.passwordHash()).isEqualTo("hashed:senha-atual");
        assertThat(f.refreshTokens.isActive(refresh)).isTrue();
    }

    @Test
    void changesPasswordWithTheRightCurrentPasswordAndEndsEverySession() {
        User ana = ana();
        f.refreshTokens.issue(ana.id());
        f.refreshTokens.issue(ana.id());

        update.execute(
                new UpdateProfileCommand(ana.id(), "Ana", null, "nova-senha-longa", "senha-atual"));

        assertThat(current.execute(ana.id()).passwordHash()).isEqualTo("hashed:nova-senha-longa");
        assertThat(f.refreshTokens.activeCount(ana.id())).isZero();
    }

    @Test
    void rejectsWrongCurrentPasswordAndChangesNothing() {
        User ana = ana();
        String refresh = f.refreshTokens.issue(ana.id());

        assertThatThrownBy(
                        () ->
                                update.execute(
                                        new UpdateProfileCommand(
                                                ana.id(),
                                                "Outro Nome",
                                                null,
                                                "nova-senha-longa",
                                                "errada")))
                .isInstanceOf(InvalidCurrentPasswordException.class)
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Senha atual incorreta.");

        User reloaded = current.execute(ana.id());
        assertThat(reloaded.passwordHash()).isEqualTo("hashed:senha-atual");
        assertThat(reloaded.displayName()).isEqualTo("Ana");
        assertThat(f.refreshTokens.isActive(refresh)).isTrue();
    }

    @Test
    void requiresCurrentPasswordWhenTheAccountAlreadyHasOne() {
        User ana = ana();

        assertThatThrownBy(
                        () ->
                                update.execute(
                                        new UpdateProfileCommand(
                                                ana.id(), "Ana", null, "nova-senha-longa", " ")))
                .isInstanceOf(InvalidCurrentPasswordException.class)
                .hasMessage("Informe a senha atual.");
        assertThat(current.execute(ana.id()).passwordHash()).isEqualTo("hashed:senha-atual");
    }

    @Test
    void socialAccountDefinesAPasswordWithoutCurrentPasswordAndEndsEverySession() {
        User bob = socialBob();
        f.refreshTokens.issue(bob.id());

        update.execute(new UpdateProfileCommand(bob.id(), "Bob", null, "primeira-senha", null));

        User reloaded = current.execute(bob.id());
        assertThat(reloaded.hasPassword()).isTrue();
        assertThat(reloaded.passwordHash()).isEqualTo("hashed:primeira-senha");
        assertThat(f.refreshTokens.activeCount(bob.id())).isZero();
    }

    @Test
    void validatesPasswordAndUser() {
        User ana = ana();

        assertThatThrownBy(
                        () ->
                                update.execute(
                                        new UpdateProfileCommand(
                                                ana.id(), "Ana", null, "curta", "senha-atual")))
                .isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(
                        () ->
                                update.execute(
                                        new UpdateProfileCommand(
                                                UUID.randomUUID(), "X", null, null, null)))
                .isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> current.execute(UUID.randomUUID()))
                .isInstanceOf(UserNotFoundException.class);
    }

    private User anaWithTemporaryPassword() {
        User ana = ana();
        ana.resetPassword(f.hasher.hash("Provisoria123456"));
        return f.users.save(ana);
    }

    @Test
    void aMarkedAccountCannotKeepTheTemporaryPassword() {
        User ana = anaWithTemporaryPassword();
        String refresh = f.refreshTokens.issue(ana.id());
        f.users.resetSaveCount();

        assertThatThrownBy(
                        () ->
                                update.execute(
                                        new UpdateProfileCommand(
                                                ana.id(),
                                                "Ana",
                                                null,
                                                "Provisoria123456",
                                                "Provisoria123456")))
                .isInstanceOf(NewPasswordSameAsTemporaryException.class)
                .hasMessage("A senha nova precisa ser diferente da provisória.");

        User reloaded = current.execute(ana.id());
        assertThat(reloaded.mustChangePassword()).isTrue();
        assertThat(reloaded.passwordHash()).isEqualTo("hashed:Provisoria123456");
        assertThat(f.refreshTokens.isActive(refresh)).isTrue();
        assertThat(f.users.saveCount()).isZero();
    }

    @Test
    void aNewPasswordClearsTheMarkAndEndsEverySession() {
        User ana = anaWithTemporaryPassword();
        f.refreshTokens.issue(ana.id());

        User answered =
                update.execute(
                        new UpdateProfileCommand(
                                ana.id(), "Ana", null, "minha-senha-nova", "Provisoria123456"));

        User reloaded = current.execute(ana.id());
        assertThat(reloaded.mustChangePassword()).isFalse();
        assertThat(reloaded.passwordHash()).isEqualTo("hashed:minha-senha-nova");
        assertThat(answered.mustChangePassword()).isFalse();
        assertThat(f.refreshTokens.activeCount(ana.id())).isZero();
        assertThat(f.transactions.calls()).isEqualTo(1);
    }

    @Test
    void theTemporaryPasswordIsStillRequiredAsTheCurrentOne() {
        User ana = anaWithTemporaryPassword();

        assertThatThrownBy(
                        () ->
                                update.execute(
                                        new UpdateProfileCommand(
                                                ana.id(),
                                                "Ana",
                                                null,
                                                "minha-senha-nova",
                                                "senha-atual")))
                .isInstanceOf(InvalidCurrentPasswordException.class);
        assertThat(current.execute(ana.id()).mustChangePassword()).isTrue();
    }

    @Test
    void changingOnlyTheNameKeepsTheMark() {
        User ana = anaWithTemporaryPassword();
        String refresh = f.refreshTokens.issue(ana.id());

        update.execute(new UpdateProfileCommand(ana.id(), "Ana Souza", null, null, null));

        User reloaded = current.execute(ana.id());
        assertThat(reloaded.displayName()).isEqualTo("Ana Souza");
        assertThat(reloaded.mustChangePassword()).isTrue();
        assertThat(reloaded.passwordHash()).isEqualTo("hashed:Provisoria123456");
        assertThat(f.refreshTokens.isActive(refresh)).isTrue();
    }

    @Test
    void withoutTheMarkTheCurrentPasswordCanBeRepeated() {
        User ana = ana();

        update.execute(
                new UpdateProfileCommand(ana.id(), "Ana", null, "senha-atual", "senha-atual"));

        assertThat(current.execute(ana.id()).passwordHash()).isEqualTo("hashed:senha-atual");
    }

    @Test
    void aResetWrittenAfterTheReadIsNotUndoneByANameChange() {
        InMemoryUserRepository users =
                new InMemoryUserRepository() {
                    @Override
                    public Optional<User> findById(UUID id) {
                        Optional<User> stored = super.findById(id);
                        Optional<User> read = stored.map(UpdateProfileUseCaseTest::copy);
                        stored.filter(user -> !user.mustChangePassword())
                                .ifPresent(user -> user.resetPassword("hashed:Provisoria123456"));
                        return read;
                    }
                };
        UUID id = f.ids.next();
        users.save(User.register(id, "eva@example.com", "Eva", "hashed:antiga", f.clock.now()));
        UpdateProfileUseCase updateWithRace =
                new UpdateProfileUseCase(users, f.hasher, f.refreshTokens, f.transactions);

        User answered =
                updateWithRace.execute(new UpdateProfileCommand(id, "Eva Lima", null, null, null));

        User stored = users.all().getFirst();
        assertThat(stored.displayName()).isEqualTo("Eva Lima");
        assertThat(stored.passwordHash()).isEqualTo("hashed:Provisoria123456");
        assertThat(stored.mustChangePassword()).isTrue();
        assertThat(answered.mustChangePassword()).isTrue();
    }

    @Test
    void aResetWrittenAfterTheReadIsNotUndoneByAPasswordChange() {
        InMemoryUserRepository users =
                new InMemoryUserRepository() {
                    @Override
                    public Optional<User> findById(UUID id) {
                        Optional<User> stored = super.findById(id);
                        Optional<User> read = stored.map(UpdateProfileUseCaseTest::copy);
                        stored.filter(user -> !user.mustChangePassword())
                                .ifPresent(user -> user.resetPassword("hashed:Provisoria123456"));
                        return read;
                    }
                };
        UUID id = f.ids.next();
        users.save(
                User.register(
                        id, "ivo@example.com", "Ivo", f.hasher.hash("senha-atual"), f.clock.now()));
        UpdateProfileUseCase updateWithRace =
                new UpdateProfileUseCase(users, f.hasher, f.refreshTokens, f.transactions);
        users.resetSaveCount();

        assertThatThrownBy(
                        () ->
                                updateWithRace.execute(
                                        new UpdateProfileCommand(
                                                id,
                                                "Ivo Lima",
                                                null,
                                                "minha-senha-nova",
                                                "senha-atual")))
                .isInstanceOf(PasswordChangedMeanwhileException.class)
                .hasMessage("Sua senha foi alterada por outro caminho. Entre de novo.");

        User stored = users.all().getFirst();
        assertThat(stored.passwordHash()).isEqualTo("hashed:Provisoria123456");
        assertThat(stored.mustChangePassword()).isTrue();
        assertThat(stored.displayName()).isEqualTo("Ivo");
        assertThat(users.saveCount()).isZero();
    }

    @Test
    void anAccessChangeWrittenAfterTheReadIsNotUndone() {
        InMemoryUserRepository users =
                new InMemoryUserRepository() {
                    @Override
                    public Optional<User> findById(UUID id) {
                        Optional<User> stored = super.findById(id);
                        Optional<User> read = stored.map(UpdateProfileUseCaseTest::copy);
                        stored.ifPresent(
                                user -> {
                                    user.deactivate();
                                    user.promoteToAdmin();
                                });
                        return read;
                    }
                };
        UUID id = f.ids.next();
        users.save(User.register(id, "dani@example.com", "Dani", "hash", f.clock.now()));
        UpdateProfileUseCase updateWithRace =
                new UpdateProfileUseCase(users, f.hasher, f.refreshTokens, f.transactions);

        User answered =
                updateWithRace.execute(
                        new UpdateProfileCommand(id, "Dani Rocha", null, null, null));

        User stored = users.all().getFirst();
        assertThat(stored.displayName()).isEqualTo("Dani Rocha");
        assertThat(stored.active()).isFalse();
        assertThat(stored.isAdmin()).isTrue();
        assertThat(answered.active()).isFalse();
        assertThat(answered.isAdmin()).isTrue();
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
