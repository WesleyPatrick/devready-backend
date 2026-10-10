package br.com.devready.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devready.seed")
public record SeedProperties(boolean enabled, String location) {
    public SeedProperties {
        if (location == null || location.isBlank()) {
            location = "classpath:seed";
        }
    }
}
