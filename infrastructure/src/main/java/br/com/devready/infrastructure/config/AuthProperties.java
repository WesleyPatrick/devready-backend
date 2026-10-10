package br.com.devready.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devready.auth")
public record AuthProperties(Duration refreshTokenTtl, Duration oneTimeCodeTtl) {
    public AuthProperties {
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(7);
        }
        if (oneTimeCodeTtl == null) {
            oneTimeCodeTtl = Duration.ofSeconds(60);
        }
    }
}
