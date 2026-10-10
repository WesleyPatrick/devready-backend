package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class UserInactiveException extends ApplicationException {
    public UserInactiveException() {
        super("Esta conta está desativada.");
    }
}
