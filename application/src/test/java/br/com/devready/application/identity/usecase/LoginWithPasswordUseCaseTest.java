package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.dto.AuthTokens;
import br.com.devready.application.identity.exception.InvalidCredentialsException;
import br.com.devready.application.identity.exception.UserInactiveException;
import br.com.devready.application.identity.fake.IdentityFixtures;
import br.com.devready.domain.identity.SocialAccount;
import br.com.devready.domain.identity.SocialProvider;
import br.com.devready.domain.identity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoginWithPasswordUseCaseTest {

    private final IdentityFixtures f = new IdentityFixtures();
    private final LoginWithPasswordUseCase login =
            new LoginWithPasswordUseCase(f.users, f.hasher, f.issueSession);

    @BeforeEach
    void seedUser() {
        f.users.save(
                User.register(
                        f.ids.next(),
                        "ana@example.com",
                        "Ana",
                        f.hasher.hash("senha-forte"),
                        f.clock.now()));
    }

    @Test
    void returnsTokensForValidCredentials() {
        AuthTokens tokens = login.execute("ANA@example.com", "senha-forte");

        assertThat(tokens.user().email()).isEqualTo("ana@example.com");
        assertThat(f.refreshTokens.isActive(tokens.refreshToken())).isTrue();
    }

    @Test
    void rejectsWrongPasswordUnknownEmailAndSocialOnlyAccounts() {
        f.users.save(
                User.fromSocial(
                        f.ids.next(),
                        "bob@example.com",
                        "Bob",
                        null,
                        new SocialAccount(SocialProvider.GITHUB, "gh-bob"),
                        f.clock.now()));

        assertThatThrownBy(() -> login.execute("ana@example.com", "errada"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> login.execute("ninguem@example.com", "senha-forte"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> login.execute("bob@example.com", "qualquer"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void spendsTheSameHashingTimeWhenThereIsNoPasswordToCompare() {
        f.users.save(
                User.fromSocial(
                        f.ids.next(),
                        "bob@example.com",
                        "Bob",
                        null,
                        new SocialAccount(SocialProvider.GITHUB, "gh-bob"),
                        f.clock.now()));

        assertThatThrownBy(() -> login.execute("ninguem@example.com", "senha-forte"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(f.hasher.dummyMatches()).isEqualTo(1);
        assertThatThrownBy(() -> login.execute("bob@example.com", "senha-forte"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(f.hasher.dummyMatches()).isEqualTo(2);

        assertThatThrownBy(() -> login.execute("ana@example.com", "errada"))
                .isInstanceOf(InvalidCredentialsException.class);
        assertThat(f.hasher.dummyMatches()).isEqualTo(2);
    }

    @Test
    void rejectsInactiveUser() {
        User ana = f.users.findByEmail("ana@example.com").orElseThrow();
        ana.deactivate();
        f.users.save(ana);

        assertThatThrownBy(() -> login.execute("ana@example.com", "senha-forte"))
                .isInstanceOf(UserInactiveException.class);
    }
}
