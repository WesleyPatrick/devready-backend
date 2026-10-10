package br.com.devready.api.identity;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitHubApiEmailResolver implements GitHubEmailResolver {

    private final RestClient client;

    public GitHubApiEmailResolver() {
        this(RestClient.builder().requestFactory(requestFactory()));
    }

    GitHubApiEmailResolver(RestClient.Builder builder) {
        this.client = builder.baseUrl("https://api.github.com").build();
    }

    private static SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    @Override
    public Optional<String> primaryEmail(String accessToken) {
        List<Map<String, Object>> emails =
                client.get()
                        .uri("/user/emails")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("Accept", "application/vnd.github+json")
                        .retrieve()
                        .body(new ParameterizedTypeReference<>() {});
        if (emails == null) {
            return Optional.empty();
        }
        return emails.stream()
                .filter(
                        e ->
                                Boolean.TRUE.equals(e.get("primary"))
                                        && Boolean.TRUE.equals(e.get("verified")))
                .map(e -> String.valueOf(e.get("email")))
                .findFirst();
    }
}
