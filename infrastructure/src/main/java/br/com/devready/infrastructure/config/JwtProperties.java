package br.com.devready.infrastructure.config;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devready.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl) {

    private static final List<String> PLACEHOLDER_PREFIXES = List.of("dev-only", "gere-com");
    private static final Set<String> PROFILES_THAT_ACCEPT_PLACEHOLDERS = Set.of("dev", "test");

    public JwtProperties {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException(
                    "devready.jwt.secret precisa ter pelo menos 32 caracteres");
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
    }

    public void requireRealSecretOutsideDev(Collection<String> activeProfiles) {
        boolean placeholder = PLACEHOLDER_PREFIXES.stream().anyMatch(secret::startsWith);
        boolean devOrTest =
                activeProfiles.stream().anyMatch(PROFILES_THAT_ACCEPT_PLACEHOLDERS::contains);
        if (placeholder && !devOrTest) {
            throw new IllegalStateException(
                    "JWT_SECRET ainda é o valor de exemplo. Gere um com `openssl rand -base64 48`"
                            + " e defina a variável, ou rode com o perfil dev"
                            + " (SPRING_PROFILES_ACTIVE=dev) na sua máquina.");
        }
    }
}
