package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.dto.AuthTokens;
import br.com.devready.application.identity.dto.RegisterUserCommand;
import br.com.devready.application.identity.exception.EmailAlreadyRegisteredException;
import br.com.devready.application.identity.port.PasswordHasher;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.identity.service.IssueSession;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.domain.identity.PasswordPolicy;
import br.com.devready.domain.identity.User;

public class RegisterUserUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final IssueSession issueSession;
    private final IdGenerator ids;
    private final Clock clock;

    public RegisterUserUseCase(
            UserRepository users,
            PasswordHasher hasher,
            IssueSession issueSession,
            IdGenerator ids,
            Clock clock) {
        this.users = users;
        this.hasher = hasher;
        this.issueSession = issueSession;
        this.ids = ids;
        this.clock = clock;
    }

    public AuthTokens execute(RegisterUserCommand command) {
        PasswordPolicy.validate(command.password());
        String email = User.normalizeEmail(command.email());
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException(email);
        }
        User user =
                User.register(
                        ids.next(),
                        email,
                        command.displayName(),
                        hasher.hash(command.password()),
                        clock.now());
        users.save(user);
        return issueSession.forUser(user);
    }
}
