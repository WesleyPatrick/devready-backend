package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.dto.UpdateProfileCommand;
import br.com.devready.application.identity.exception.InvalidCurrentPasswordException;
import br.com.devready.application.identity.exception.NewPasswordSameAsTemporaryException;
import br.com.devready.application.identity.exception.PasswordChangedMeanwhileException;
import br.com.devready.application.identity.exception.UserNotFoundException;
import br.com.devready.application.identity.port.PasswordHasher;
import br.com.devready.application.identity.port.RefreshTokenStore;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.shared.Transactions;
import br.com.devready.domain.identity.PasswordPolicy;
import br.com.devready.domain.identity.User;

public class UpdateProfileUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final RefreshTokenStore refreshTokens;
    private final Transactions transactions;

    public UpdateProfileUseCase(
            UserRepository users,
            PasswordHasher hasher,
            RefreshTokenStore refreshTokens,
            Transactions transactions) {
        this.users = users;
        this.hasher = hasher;
        this.refreshTokens = refreshTokens;
        this.transactions = transactions;
    }

    public User execute(UpdateProfileCommand command) {
        User user = users.findById(command.userId()).orElseThrow(UserNotFoundException::new);
        boolean changesPassword = isPresent(command.newPassword());
        String newHash = null;
        if (changesPassword) {
            if (user.hasPassword()) {
                requireCurrentPassword(user, command.currentPassword());
            }
            PasswordPolicy.validate(command.newPassword());
            if (user.mustChangePassword()
                    && hasher.matches(command.newPassword(), user.passwordHash())) {
                throw new NewPasswordSameAsTemporaryException();
            }
            newHash = hasher.hash(command.newPassword());
        }
        user.updateProfile(command.displayName(), command.avatarUrl());
        String checkedHash = user.passwordHash();
        String hashToStore = newHash;
        transactions.inTransaction(
                () -> {
                    if (changesPassword) {
                        refreshTokens.revokeAll(user.id());
                        if (!users.replacePassword(user.id(), checkedHash, hashToStore)) {
                            throw new PasswordChangedMeanwhileException();
                        }
                    }
                    users.saveProfile(user);
                    return null;
                });
        return users.findById(user.id()).orElseThrow(UserNotFoundException::new);
    }

    private void requireCurrentPassword(User user, String currentPassword) {
        if (!isPresent(currentPassword)) {
            throw InvalidCurrentPasswordException.missing();
        }
        if (!hasher.matches(currentPassword, user.passwordHash())) {
            throw InvalidCurrentPasswordException.incorrect();
        }
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
