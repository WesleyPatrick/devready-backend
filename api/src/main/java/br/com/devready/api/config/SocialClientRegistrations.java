package br.com.devready.api.config;

import br.com.devready.domain.identity.SocialProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

public final class SocialClientRegistrations
        implements ClientRegistrationRepository, Iterable<ClientRegistration> {

    private final Map<String, ClientRegistration> byId;

    private SocialClientRegistrations(Map<String, ClientRegistration> byId) {
        this.byId = byId;
    }

    public static SocialClientRegistrations from(SocialLoginProperties properties) {
        Map<String, ClientRegistration> byId = new LinkedHashMap<>();
        SocialLoginProperties.Client google = properties == null ? null : properties.google();
        if (google != null && google.configured()) {
            byId.put(
                    idOf(SocialProvider.GOOGLE),
                    CommonOAuth2Provider.GOOGLE
                            .getBuilder(idOf(SocialProvider.GOOGLE))
                            .clientId(google.clientId())
                            .clientSecret(google.clientSecret())
                            .scope("openid", "profile", "email")
                            .build());
        }
        SocialLoginProperties.Client github = properties == null ? null : properties.github();
        if (github != null && github.configured()) {
            byId.put(
                    idOf(SocialProvider.GITHUB),
                    CommonOAuth2Provider.GITHUB
                            .getBuilder(idOf(SocialProvider.GITHUB))
                            .clientId(github.clientId())
                            .clientSecret(github.clientSecret())
                            .scope("read:user", "user:email")
                            .build());
        }
        return new SocialClientRegistrations(byId);
    }

    public List<SocialProvider> providers() {
        List<SocialProvider> providers = new ArrayList<>();
        for (SocialProvider provider : SocialProvider.values()) {
            if (byId.containsKey(idOf(provider))) {
                providers.add(provider);
            }
        }
        return providers;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        return byId.get(registrationId);
    }

    @Override
    public Iterator<ClientRegistration> iterator() {
        return byId.values().iterator();
    }

    private static String idOf(SocialProvider provider) {
        return provider.name().toLowerCase(Locale.ROOT);
    }
}
