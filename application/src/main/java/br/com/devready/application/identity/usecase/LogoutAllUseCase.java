package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.port.RefreshTokenStore;
import java.util.UUID;

public class LogoutAllUseCase {

    private final RefreshTokenStore refreshTokens;

    public LogoutAllUseCase(RefreshTokenStore refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    public void execute(UUID userId) {
        refreshTokens.revokeAll(userId);
    }
}
