package br.com.devready.application.identity.port;

import br.com.devready.domain.identity.User;
import java.time.Duration;

public interface TokenIssuer {
    AccessToken issue(User user);

    record AccessToken(String value, Duration expiresIn) {}
}
