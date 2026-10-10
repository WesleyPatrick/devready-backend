package br.com.devready.application.session.usecase;

import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.session.service.SessionAccess;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionStatus;
import java.util.UUID;

public class FinishSessionUseCase {

    private final SessionRepository sessions;
    private final FinalizeSessionUseCase finalizeSession;

    public FinishSessionUseCase(
            SessionRepository sessions, FinalizeSessionUseCase finalizeSession) {
        this.sessions = sessions;
        this.finalizeSession = finalizeSession;
    }

    public Session execute(UUID userId, UUID sessionId) {
        Session session = SessionAccess.owned(sessions, userId, sessionId);
        if (session.status() == SessionStatus.IN_PROGRESS) {
            sessions.submit(session.id());
        }
        finalizeSession.execute(session.id());
        return sessions.findById(session.id()).orElseThrow();
    }
}
