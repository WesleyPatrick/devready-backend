package br.com.devready.application.career.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class InvalidCareerSeedException extends ApplicationException {
    public InvalidCareerSeedException(String message) {
        super(message);
    }

    public InvalidCareerSeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
