package br.com.devready.infrastructure.identity.security;

import br.com.devready.infrastructure.config.JwtProperties;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public record JwtSecretKey(SecretKey value) {
    public JwtSecretKey(JwtProperties properties) {
        this(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    }
}
