package br.com.devready.application.practice.exception;

import br.com.devready.application.shared.exception.ApplicationException;
import java.util.UUID;

public abstract class ProblemInExamException extends ApplicationException {
    private final UUID sessionId;

    protected ProblemInExamException(String message, UUID sessionId) {
        super(message);
        this.sessionId = sessionId;
    }

    public UUID sessionId() {
        return sessionId;
    }
}
