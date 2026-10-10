package br.com.devready.application.identity.service;

import br.com.devready.application.identity.dto.AuthTokens;
import br.com.devready.application.identity.exception.UserInactiveException;
import br.com.devready.application.identity.port.RefreshTokenStore;
import br.com.devready.application.identity.port.TokenIssuer;
import br.com.devready.domain.identity.User;

public class IssueSession {

    private final TokenIssuer tokenIssuer;
    private final RefreshTokenStore refreshTokenStore;

    public IssueSession(TokenIssuer tokenIssuer, RefreshTokenStore refreshTokenStore) {
        this.tokenIssuer = tokenIssuer;
        this.refreshTokenStore = refreshTokenStore;
    }

    public AuthTokens forUser(User user) {
        if (!user.active()) {
            throw new UserInactiveException();
        }
        TokenIssuer.AccessToken access = tokenIssuer.issue(user);
        String refresh = refreshTokenStore.issue(user.id());
        return new AuthTokens(access.value(), refresh, access.expiresIn().toSeconds(), user);
    }
}
