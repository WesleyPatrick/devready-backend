package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;
import br.com.devready.domain.session.SessionFormat;
import java.util.UUID;

public class SessionInProgressException extends ApplicationException {
    private final UUID sessionId;

    public SessionInProgressException(UUID sessionId, SessionFormat format) {
        super(
                format == SessionFormat.SIMULATION
                        ? "Você já tem uma simulação em andamento."
                        : "Você já tem uma lição em andamento.");
        this.sessionId = sessionId;
    }

    public UUID sessionId() {
        return sessionId;
    }
}
