package br.com.devready.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devready.auth.rate-limit")
public record RateLimitProperties(boolean enabled, int requestsPerMinute) {
    public RateLimitProperties {
        if (requestsPerMinute <= 0) {
            requestsPerMinute = 10;
        }
    }
}
