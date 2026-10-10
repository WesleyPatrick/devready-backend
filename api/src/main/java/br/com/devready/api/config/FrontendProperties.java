package br.com.devready.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devready")
public record FrontendProperties(String frontendUrl) {
    public FrontendProperties {
        if (frontendUrl == null || frontendUrl.isBlank()) {
            throw new IllegalArgumentException("devready.frontend-url é obrigatório");
        }
        frontendUrl =
                frontendUrl.endsWith("/")
                        ? frontendUrl.substring(0, frontendUrl.length() - 1)
                        : frontendUrl;
    }
}
