package br.com.devready.infrastructure.identity.security;

import br.com.devready.application.identity.port.TokenIssuer;
import br.com.devready.application.shared.Clock;
import br.com.devready.domain.identity.User;
import br.com.devready.infrastructure.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

public class NimbusTokenIssuer implements TokenIssuer {

    public static final String PASSWORD_CHANGE_CLAIM = "pwd_change";

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock clock;

    public NimbusTokenIssuer(JwtSecretKey key, JwtProperties properties, Clock clock) {
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key.value()));
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public AccessToken issue(User user) {
        Instant now = clock.now();
        JwtClaimsSet.Builder claims =
                JwtClaimsSet.builder()
                        .issuer("devready")
                        .subject(user.id().toString())
                        .issuedAt(now)
                        .expiresAt(now.plus(properties.accessTokenTtl()))
                        .claim("email", user.email())
                        .claim("role", user.role().name());
        if (user.mustChangePassword()) {
            claims.claim(PASSWORD_CHANGE_CLAIM, true);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value =
                encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new AccessToken(value, properties.accessTokenTtl());
    }
}
