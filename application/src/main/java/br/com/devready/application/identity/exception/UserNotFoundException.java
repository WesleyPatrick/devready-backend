package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class UserNotFoundException extends ApplicationException {
    public UserNotFoundException() {
        super("Usuário não encontrado.");
    }
}
