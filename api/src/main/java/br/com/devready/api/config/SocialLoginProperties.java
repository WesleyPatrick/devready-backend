package br.com.devready.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devready.social")
public record SocialLoginProperties(Client google, Client github) {

    public record Client(String clientId, String clientSecret) {

        public boolean configured() {
            return clientId != null
                    && !clientId.isBlank()
                    && clientSecret != null
                    && !clientSecret.isBlank();
        }
    }
}
