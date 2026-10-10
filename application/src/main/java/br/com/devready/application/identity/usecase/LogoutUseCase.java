package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.port.RefreshTokenStore;

public class LogoutUseCase {

    private final RefreshTokenStore refreshTokens;

    public LogoutUseCase(RefreshTokenStore refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    public void execute(String refreshToken) {
        refreshTokens.revoke(refreshToken);
    }
}
