package br.com.devready.application.evaluation.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class ExecutionUnavailableException extends ApplicationException {
    public ExecutionUnavailableException(String message) {
        super(message);
    }

    public ExecutionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
