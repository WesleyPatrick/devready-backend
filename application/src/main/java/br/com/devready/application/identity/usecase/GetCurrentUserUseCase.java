package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.exception.UserNotFoundException;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.domain.identity.User;
import java.util.UUID;

public class GetCurrentUserUseCase {

    private final UserRepository users;

    public GetCurrentUserUseCase(UserRepository users) {
        this.users = users;
    }

    public User execute(UUID userId) {
        return users.findById(userId).orElseThrow(UserNotFoundException::new);
    }
}
