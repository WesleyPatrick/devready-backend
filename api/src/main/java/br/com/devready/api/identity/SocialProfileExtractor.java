package br.com.devready.api.identity;

import br.com.devready.application.identity.dto.SocialLoginCommand;
import br.com.devready.domain.identity.SocialProvider;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class SocialProfileExtractor {

    private SocialProfileExtractor() {}

    public static SocialLoginCommand extract(
            String registrationId,
            Map<String, Object> attributes,
            Supplier<Optional<String>> emailFallback) {
        return switch (registrationId) {
            case "google" ->
                    new SocialLoginCommand(
                            SocialProvider.GOOGLE,
                            text(attributes.get("sub")),
                            Boolean.TRUE.equals(attributes.get("email_verified"))
                                    ? text(attributes.get("email"))
                                    : null,
                            text(attributes.get("name")),
                            text(attributes.get("picture")));
            case "github" -> {
                String email = text(attributes.get("email"));
                if (email == null) {
                    email = emailFallback.get().orElse(null);
                }
                String name = text(attributes.get("name"));
                yield new SocialLoginCommand(
                        SocialProvider.GITHUB,
                        text(attributes.get("id")),
                        email,
                        name != null ? name : text(attributes.get("login")),
                        text(attributes.get("avatar_url")));
            }
            default ->
                    throw new IllegalArgumentException("Provedor desconhecido: " + registrationId);
        };
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
