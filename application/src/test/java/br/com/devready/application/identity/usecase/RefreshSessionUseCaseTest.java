package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.dto.AuthTokens;
import br.com.devready.application.identity.exception.InvalidRefreshTokenException;
import br.com.devready.application.identity.exception.UserInactiveException;
import br.com.devready.application.identity.fake.IdentityFixtures;
import br.com.devready.domain.identity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefreshSessionUseCaseTest {

    private final IdentityFixtures f = new IdentityFixtures();
    private final RefreshSessionUseCase refresh =
            new RefreshSessionUseCase(f.users, f.refreshTokens, f.issueSession);
    private User ana;

    @BeforeEach
    void seed() {
        ana =
                f.users.save(
                        User.register(
                                f.ids.next(), "ana@example.com", "Ana", "hash", f.clock.now()));
    }

    @Test
    void rotatesTheRefreshToken() {
        String first = f.refreshTokens.issue(ana.id());

        AuthTokens tokens = refresh.execute(first);

        assertThat(tokens.refreshToken()).isNotEqualTo(first);
        assertThat(f.refreshTokens.isActive(first)).isFalse();
        assertThat(f.refreshTokens.isActive(tokens.refreshToken())).isTrue();
        assertThat(tokens.user().id()).isEqualTo(ana.id());
    }

    @Test
    void unknownTokenIsRejected() {
        assertThatThrownBy(() -> refresh.execute("nope"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void reusedTokenRevokesEveryActiveSessionOfTheUser() {
        String first = f.refreshTokens.issue(ana.id());
        AuthTokens rotated = refresh.execute(first);
        String other = f.refreshTokens.issue(ana.id());

        assertThatThrownBy(() -> refresh.execute(first))
                .isInstanceOf(InvalidRefreshTokenException.class);
        assertThat(f.refreshTokens.isActive(rotated.refreshToken())).isFalse();
        assertThat(f.refreshTokens.isActive(other)).isFalse();
    }

    @Test
    void inactiveUserCannotRefresh() {
        String token = f.refreshTokens.issue(ana.id());
        ana.deactivate();
        f.users.save(ana);

        assertThatThrownBy(() -> refresh.execute(token)).isInstanceOf(UserInactiveException.class);
        assertThat(f.refreshTokens.isActive(token)).isFalse();
    }
}
