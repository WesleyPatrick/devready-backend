package br.com.devready.application.identity.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.identity.fake.IdentityFixtures;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LogoutUseCaseTest {

    private final IdentityFixtures f = new IdentityFixtures();
    private final UUID userId = f.ids.next();

    @Test
    void logoutRevokesOnlyTheGivenToken() {
        String a = f.refreshTokens.issue(userId);
        String b = f.refreshTokens.issue(userId);

        new LogoutUseCase(f.refreshTokens).execute(a);

        assertThat(f.refreshTokens.isActive(a)).isFalse();
        assertThat(f.refreshTokens.isActive(b)).isTrue();
    }

    @Test
    void logoutAllRevokesEveryTokenOfTheUser() {
        f.refreshTokens.issue(userId);
        f.refreshTokens.issue(userId);
        String someoneElse = f.refreshTokens.issue(f.ids.next());

        new LogoutAllUseCase(f.refreshTokens).execute(userId);

        assertThat(f.refreshTokens.activeCount(userId)).isZero();
        assertThat(f.refreshTokens.isActive(someoneElse)).isTrue();
    }
}
