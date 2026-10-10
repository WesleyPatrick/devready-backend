package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.dto.ChangeUserAccessCommand;
import br.com.devready.application.identity.exception.CannotChangeOwnAccountException;
import br.com.devready.application.identity.exception.LastActiveAdminException;
import br.com.devready.application.identity.exception.NotAnActiveAdminException;
import br.com.devready.application.identity.exception.NothingToChangeException;
import br.com.devready.application.identity.exception.UserNotFoundException;
import br.com.devready.application.identity.port.RefreshTokenStore;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.shared.Transactions;
import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.User;
import java.util.UUID;

public class ChangeUserAccessUseCase {

    private final UserRepository users;
    private final RefreshTokenStore refreshTokens;
    private final Transactions transactions;

    public ChangeUserAccessUseCase(
            UserRepository users, RefreshTokenStore refreshTokens, Transactions transactions) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.transactions = transactions;
    }

    public User execute(UUID actorId, UUID targetId, ChangeUserAccessCommand command) {
        if (command.isEmpty()) {
            throw new NothingToChangeException();
        }
        if (actorId.equals(targetId)) {
            throw new CannotChangeOwnAccountException();
        }
        return transactions.inTransaction(() -> change(actorId, targetId, command));
    }

    private User change(UUID actorId, UUID targetId, ChangeUserAccessCommand command) {
        int activeAdmins = users.lockActiveAdmins();
        users.findById(actorId)
                .filter(User::canManageUsers)
                .orElseThrow(NotAnActiveAdminException::new);
        User user = users.findById(targetId).orElseThrow(UserNotFoundException::new);
        boolean active = command.active() == null ? user.active() : command.active();
        Role role = command.role() == null ? user.role() : command.role();
        boolean staysActiveAdmin = active && role == Role.ADMIN;
        if (user.isActiveAdmin() && !staysActiveAdmin && activeAdmins <= 1) {
            throw new LastActiveAdminException();
        }
        if (active == user.active() && role == user.role()) {
            return user;
        }
        if (user.active() && !active) {
            refreshTokens.revokeAll(user.id());
        }
        apply(user, active, role);
        users.saveAccess(user);
        return users.findById(user.id()).orElseThrow(UserNotFoundException::new);
    }

    private static void apply(User user, boolean active, Role role) {
        if (active) {
            user.activate();
        } else {
            user.deactivate();
        }
        if (role == Role.ADMIN) {
            user.promoteToAdmin();
        } else {
            user.demoteToCandidate();
        }
    }
}
