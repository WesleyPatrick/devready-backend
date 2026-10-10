package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class InvalidCredentialsException extends ApplicationException {
    public InvalidCredentialsException() {
        super("E-mail ou senha incorretos.");
    }

    protected InvalidCredentialsException(String message) {
        super(message);
    }
}
