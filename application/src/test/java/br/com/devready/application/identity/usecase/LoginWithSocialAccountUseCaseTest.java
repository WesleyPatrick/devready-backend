package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.dto.OneTimeCode;
import br.com.devready.application.identity.dto.SocialLoginCommand;
import br.com.devready.application.identity.exception.UserInactiveException;
import br.com.devready.application.identity.fake.IdentityFixtures;
import br.com.devready.application.identity.fake.InMemoryUserRepository;
import br.com.devready.application.shared.exception.ApplicationException;
import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.SocialAccount;
import br.com.devready.domain.identity.SocialProvider;
import br.com.devready.domain.identity.User;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LoginWithSocialAccountUseCaseTest {

    private final IdentityFixtures f = new IdentityFixtures();
    private final LoginWithSocialAccountUseCase login =
            new LoginWithSocialAccountUseCase(f.users, f.oneTimeCodes, f.ids, f.clock);
    private final SocialLoginCommand google =
            new SocialLoginCommand(
                    SocialProvider.GOOGLE, "g-1", "Ana@Example.com", "Ana", "https://img/a.png");

    @Test
    void createsUserOnFirstSocialLoginAndIssuesCode() {
        OneTimeCode code = login.execute(google);

        User created = f.users.findByEmail("ana@example.com").orElseThrow();
        assertThat(created.hasPassword()).isFalse();
        assertThat(created.hasSocialAccount(SocialProvider.GOOGLE)).isTrue();
        assertThat(f.oneTimeCodes.consume(code.value())).contains(created.id());
    }

    @Test
    void findsExistingUserByProviderId() {
        OneTimeCode first = login.execute(google);
        OneTimeCode second =
                login.execute(
                        new SocialLoginCommand(
                                SocialProvider.GOOGLE, "g-1", "other@example.com", "X", null));

        assertThat(f.users.count()).isEqualTo(1);
        assertThat(f.oneTimeCodes.consume(second.value()))
                .isEqualTo(f.oneTimeCodes.consume(first.value()));
    }

    @Test
    void linksProviderToExistingEmailAccount() {
        User ana =
                f.users.save(
                        User.register(
                                f.ids.next(), "ana@example.com", "Ana", "hash", f.clock.now()));

        login.execute(
                new SocialLoginCommand(
                        SocialProvider.GITHUB, "gh-9", "ana@example.com", "Ana GH", null));

        User reloaded = f.users.findById(ana.id()).orElseThrow();
        assertThat(reloaded.hasSocialAccount(SocialProvider.GITHUB)).isTrue();
        assertThat(reloaded.hasPassword()).isTrue();
        assertThat(reloaded.displayName()).isEqualTo("Ana");
        assertThat(f.users.count()).isEqualTo(1);
    }

    @Test
    void doesNotLinkProviderToAnInactiveAccount() {
        User ana =
                f.users.save(
                        User.register(
                                f.ids.next(), "ana@example.com", "Ana", "hash", f.clock.now()));
        ana.deactivate();
        f.users.save(ana);
        f.users.resetSaveCount();

        assertThatThrownBy(
                        () ->
                                login.execute(
                                        new SocialLoginCommand(
                                                SocialProvider.GITHUB,
                                                "gh-9",
                                                "ana@example.com",
                                                "Ana GH",
                                                null)))
                .isInstanceOf(UserInactiveException.class);

        assertThat(ana.hasSocialAccount(SocialProvider.GITHUB)).isFalse();
        assertThat(f.users.saveCount()).isZero();
    }

    @Test
    void linksByEmailRegardlessOfLetterCase() {
        User ana =
                f.users.save(
                        User.register(
                                f.ids.next(), "ana@example.com", "Ana", "hash", f.clock.now()));

        login.execute(
                new SocialLoginCommand(
                        SocialProvider.GITHUB, "gh-9", "  ANA@Example.Com ", "Ana GH", null));

        assertThat(f.users.findById(ana.id()).orElseThrow().hasSocialAccount(SocialProvider.GITHUB))
                .isTrue();
        assertThat(f.users.count()).isEqualTo(1);
    }

    @Test
    void usesProviderIdAsDisplayNameWhenNameIsMissing() {
        login.execute(
                new SocialLoginCommand(
                        SocialProvider.GITHUB, "gh-9", "bob@example.com", null, null));

        assertThat(f.users.findByEmail("bob@example.com").orElseThrow().displayName())
                .isEqualTo("bob");
    }

    @Test
    void rejectsInactiveUserAndMissingEmail() {
        User ana =
                f.users.save(
                        User.fromSocial(
                                f.ids.next(),
                                "ana@example.com",
                                "Ana",
                                null,
                                new SocialAccount(SocialProvider.GOOGLE, "g-1"),
                                f.clock.now()));
        ana.deactivate();
        f.users.save(ana);

        assertThatThrownBy(() -> login.execute(google)).isInstanceOf(UserInactiveException.class);
        assertThatThrownBy(
                        () ->
                                login.execute(
                                        new SocialLoginCommand(
                                                SocialProvider.GITHUB, "gh-2", null, "X", null)))
                .isInstanceOf(ApplicationException.class)
                .hasMessage("O provedor não informou um e-mail. Não dá para criar a conta.");
    }

    @Test
    void logsInAnAlreadyLinkedAccountEvenWhenTheProviderOmitsTheEmail() {
        login.execute(google);
        SocialLoginCommand withoutEmail =
                new SocialLoginCommand(
                        google.provider(), google.providerUserId(), null, null, null);

        OneTimeCode code = login.execute(withoutEmail);

        assertThat(f.oneTimeCodes.consume(code.value())).isPresent();
        assertThat(f.users.findByEmail("ana@example.com")).isPresent();
    }

    @Test
    void linkingDoesNotUndoWhatWasWrittenAfterTheRead() {
        InMemoryUserRepository users =
                new InMemoryUserRepository() {
                    @Override
                    public Optional<User> findByEmail(String email) {
                        Optional<User> stored = super.findByEmail(email);
                        Optional<User> read = stored.map(LoginWithSocialAccountUseCaseTest::copy);
                        stored.ifPresent(
                                user -> {
                                    user.resetPassword("hash-provisorio");
                                    user.promoteToAdmin();
                                });
                        return read;
                    }
                };
        User ana =
                users.save(
                        User.register(
                                f.ids.next(), "ana@example.com", "Ana", "hash", f.clock.now()));
        LoginWithSocialAccountUseCase loginWithRace =
                new LoginWithSocialAccountUseCase(users, f.oneTimeCodes, f.ids, f.clock);
        users.resetSaveCount();

        loginWithRace.execute(
                new SocialLoginCommand(
                        SocialProvider.GITHUB, "gh-9", "ana@example.com", "Ana GH", null));

        User stored = users.all().getFirst();
        assertThat(stored.id()).isEqualTo(ana.id());
        assertThat(stored.hasSocialAccount(SocialProvider.GITHUB)).isTrue();
        assertThat(stored.passwordHash()).isEqualTo("hash-provisorio");
        assertThat(stored.mustChangePassword()).isTrue();
        assertThat(stored.role()).isEqualTo(Role.ADMIN);
        assertThat(users.saveCount()).isZero();
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
