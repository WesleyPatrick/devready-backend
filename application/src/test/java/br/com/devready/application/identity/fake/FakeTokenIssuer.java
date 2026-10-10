package br.com.devready.application.identity.fake;

import br.com.devready.application.identity.port.TokenIssuer;
import br.com.devready.domain.identity.User;
import java.time.Duration;

public class FakeTokenIssuer implements TokenIssuer {
    @Override
    public AccessToken issue(User user) {
        return new AccessToken("access-for-" + user.id(), Duration.ofMinutes(15));
    }
}
