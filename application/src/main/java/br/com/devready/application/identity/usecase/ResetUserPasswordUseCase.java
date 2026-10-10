package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.exception.CannotChangeOwnAccountException;
import br.com.devready.application.identity.exception.NotAnActiveAdminException;
import br.com.devready.application.identity.exception.UserNotFoundException;
import br.com.devready.application.identity.port.PasswordGenerator;
import br.com.devready.application.identity.port.PasswordHasher;
import br.com.devready.application.identity.port.RefreshTokenStore;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.shared.Transactions;
import br.com.devready.domain.identity.PasswordPolicy;
import br.com.devready.domain.identity.User;
import java.util.UUID;

public class ResetUserPasswordUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final PasswordGenerator passwords;
    private final RefreshTokenStore refreshTokens;
    private final Transactions transactions;

    public ResetUserPasswordUseCase(
            UserRepository users,
            PasswordHasher hasher,
            PasswordGenerator passwords,
            RefreshTokenStore refreshTokens,
            Transactions transactions) {
        this.users = users;
        this.hasher = hasher;
        this.passwords = passwords;
        this.refreshTokens = refreshTokens;
        this.transactions = transactions;
    }

    public String execute(UUID actorId, UUID targetId) {
        if (actorId.equals(targetId)) {
            throw new CannotChangeOwnAccountException();
        }
        return transactions.inTransaction(() -> reset(actorId, targetId));
    }

    private String reset(UUID actorId, UUID targetId) {
        users.lockActiveAdmins();
        users.findById(actorId)
                .filter(User::canManageUsers)
                .orElseThrow(NotAnActiveAdminException::new);
        User user = users.findById(targetId).orElseThrow(UserNotFoundException::new);
        String temporary = passwords.generate();
        PasswordPolicy.validate(temporary);
        String hash = hasher.hash(temporary);
        refreshTokens.revokeAll(user.id());
        user.resetPassword(hash);
        users.saveCredentials(user);
        return temporary;
    }
}
