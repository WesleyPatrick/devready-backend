package br.com.devready.application.catalog.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class InvalidSeedException extends ApplicationException {
    public InvalidSeedException(String message) {
        super(message);
    }

    public InvalidSeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
