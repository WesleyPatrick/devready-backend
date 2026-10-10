package br.com.devready.api;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

public final class TestJwts {

    public static final String TEST_SECRET = "test-secret-test-secret-test-secret-test-secret";

    private TestJwts() {}

    public static String sign(String secret, String issuer, Instant expiresAt) {
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        JwtClaimsSet claims =
                JwtClaimsSet.builder()
                        .issuer(issuer)
                        .subject(UUID.randomUUID().toString())
                        .issuedAt(expiresAt.minus(Duration.ofMinutes(15)))
                        .expiresAt(expiresAt)
                        .claim("role", "CANDIDATE")
                        .build();
        return encoder.encode(
                        JwtEncoderParameters.from(
                                JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    public static String expired() {
        return sign(TEST_SECRET, "devready", Instant.now().minus(Duration.ofMinutes(5)));
    }
}
