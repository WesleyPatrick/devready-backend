package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.dto.AuthTokens;
import br.com.devready.application.identity.exception.InvalidRefreshTokenException;
import br.com.devready.application.identity.port.RefreshTokenStore;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.identity.service.IssueSession;
import br.com.devready.domain.identity.User;

public class RefreshSessionUseCase {

    private final UserRepository users;
    private final RefreshTokenStore refreshTokens;
    private final IssueSession issueSession;

    public RefreshSessionUseCase(
            UserRepository users, RefreshTokenStore refreshTokens, IssueSession issueSession) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.issueSession = issueSession;
    }

    public AuthTokens execute(String refreshToken) {
        RefreshTokenStore.ConsumeResult result = refreshTokens.consume(refreshToken);
        switch (result.status()) {
            case UNKNOWN -> throw new InvalidRefreshTokenException();
            case REUSED -> {
                refreshTokens.revokeAll(result.userId());
                throw new InvalidRefreshTokenException();
            }
            case VALID -> {
                User user =
                        users.findById(result.userId())
                                .orElseThrow(InvalidRefreshTokenException::new);
                return issueSession.forUser(user);
            }
        }
        throw new InvalidRefreshTokenException();
    }
}
