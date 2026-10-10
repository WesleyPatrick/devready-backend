package br.com.devready.api.identity;

import java.util.Optional;

public interface GitHubEmailResolver {
    Optional<String> primaryEmail(String accessToken);
}
