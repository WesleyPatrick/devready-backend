package br.com.devready.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtPropertiesTest {

    private static final String DEV_DEFAULT =
            "dev-only-secret-change-me-before-any-deploy-0123456789";
    private static final String ENV_EXAMPLE =
            "gere-com-openssl-rand-base64-48-e-cole-aqui-com-pelo-menos-32-caracteres";
    private static final String REAL = "Xr2l6y0m8Qe4VbT1sPz9KcJ3wHn7Ud5aLf0GiRkYoE2qMx8B";

    private static JwtProperties jwt(String secret) {
        return new JwtProperties(secret, Duration.ofMinutes(15));
    }

    @Test
    void refusesPlaceholderSecretsOutsideDevAndTest() {
        assertThatThrownBy(() -> jwt(DEV_DEFAULT).requireRealSecretOutsideDev(List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
        assertThatThrownBy(() -> jwt(ENV_EXAMPLE).requireRealSecretOutsideDev(List.of("prod")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT_SECRET");
    }

    @Test
    void acceptsPlaceholdersInDevAndTest() {
        assertThatCode(() -> jwt(DEV_DEFAULT).requireRealSecretOutsideDev(List.of("dev")))
                .doesNotThrowAnyException();
        assertThatCode(() -> jwt(ENV_EXAMPLE).requireRealSecretOutsideDev(List.of("test")))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsRealSecretAnywhere() {
        assertThatCode(() -> jwt(REAL).requireRealSecretOutsideDev(List.of()))
                .doesNotThrowAnyException();
    }
}
