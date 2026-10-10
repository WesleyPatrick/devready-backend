package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.dto.AuthTokens;
import br.com.devready.application.identity.exception.InvalidCredentialsException;
import br.com.devready.application.identity.port.PasswordHasher;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.identity.service.IssueSession;
import br.com.devready.domain.identity.InvalidEmailException;
import br.com.devready.domain.identity.User;

public class LoginWithPasswordUseCase {

    private final UserRepository users;
    private final PasswordHasher hasher;
    private final IssueSession issueSession;

    public LoginWithPasswordUseCase(
            UserRepository users, PasswordHasher hasher, IssueSession issueSession) {
        this.users = users;
        this.hasher = hasher;
        this.issueSession = issueSession;
    }

    public AuthTokens execute(String email, String password) {
        String normalized;
        try {
            normalized = User.normalizeEmail(email);
        } catch (InvalidEmailException e) {
            throw new InvalidCredentialsException();
        }
        User user = users.findByEmail(normalized).filter(User::hasPassword).orElse(null);
        if (user == null) {
            hasher.dummyMatch(password);
            throw new InvalidCredentialsException();
        }
        if (!hasher.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return issueSession.forUser(user);
    }
}
