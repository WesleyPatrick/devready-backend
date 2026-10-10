package br.com.devready.infrastructure.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.identity.User;
import br.com.devready.infrastructure.config.JwtProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

class NimbusTokenIssuerTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef";
    private final JwtProperties properties = new JwtProperties(SECRET, Duration.ofMinutes(15));
    private final Instant now = Instant.ofEpochSecond(946_684_800L);
    private final NimbusTokenIssuer issuer =
            new NimbusTokenIssuer(new JwtSecretKey(properties), properties, () -> now);

    @Test
    void issuesHs256TokenWithSubjectEmailRoleAndExpiry() {
        User user = User.register(UUID.randomUUID(), "ana@example.com", "Ana", "hash", now);
        user.promoteToAdmin();

        var token = issuer.issue(user);

        assertThat(token.expiresIn()).isEqualTo(Duration.ofMinutes(15));
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(new JwtSecretKey(properties).value())
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();
        decoder.setJwtValidator(
                token2 ->
                        org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
                                .success());
        Jwt jwt = decoder.decode(token.value());
        assertThat(jwt.getSubject()).isEqualTo(user.id().toString());
        assertThat(jwt.getClaimAsString("email")).isEqualTo("ana@example.com");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(jwt.getClaimAsString("iss")).isEqualTo("devready");
        assertThat(jwt.getIssuedAt()).isEqualTo(now);
        assertThat(jwt.getExpiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
        assertThat(jwt.hasClaim("pwd_change")).isFalse();
    }

    @Test
    void marksTheTokenOfAnAccountWithATemporaryPassword() {
        User user = User.register(UUID.randomUUID(), "bia@example.com", "Bia", "hash", now);
        user.resetPassword("hash-provisorio");

        Jwt jwt = decode(issuer.issue(user).value());

        assertThat(jwt.getClaims()).containsEntry("pwd_change", true);

        user.changePassword("hash-novo");
        assertThat(decode(issuer.issue(user).value()).hasClaim("pwd_change")).isFalse();
    }

    private Jwt decode(String token) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(new JwtSecretKey(properties).value())
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();
        decoder.setJwtValidator(
                ignored ->
                        org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
                                .success());
        return decoder.decode(token);
    }
}
