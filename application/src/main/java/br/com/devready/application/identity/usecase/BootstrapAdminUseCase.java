package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.dto.BootstrapAdminOutcome;
import br.com.devready.application.identity.port.PasswordHasher;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.domain.identity.PasswordPolicy;
import br.com.devready.domain.identity.User;

public class BootstrapAdminUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final IdGenerator ids;
    private final Clock clock;

    public BootstrapAdminUseCase(
            UserRepository users, PasswordHasher hasher, IdGenerator ids, Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.ids = ids;
        this.clock = clock;
    }

    public BootstrapAdminOutcome execute(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            return BootstrapAdminOutcome.SKIPPED;
        }
        PasswordPolicy.validate(password);
        String normalized = User.normalizeEmail(email);
        var existing = users.findByEmail(normalized);
        if (existing.isPresent()) {
            return existing.get().isAdmin()
                    ? BootstrapAdminOutcome.ALREADY_ADMIN
                    : BootstrapAdminOutcome.EMAIL_BELONGS_TO_CANDIDATE;
        }
        User admin =
                User.register(
                        ids.next(),
                        normalized,
                        "Administrador",
                        hasher.hash(password),
                        clock.now());
        admin.promoteToAdmin();
        users.save(admin);
        return BootstrapAdminOutcome.CREATED;
    }
}
