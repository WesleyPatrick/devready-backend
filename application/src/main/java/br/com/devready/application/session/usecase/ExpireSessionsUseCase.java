package br.com.devready.application.session.usecase;

import br.com.devready.application.session.dto.ExpiryRound;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.shared.Clock;
import br.com.devready.domain.session.Session;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ExpireSessionsUseCase {

    private final SessionRepository sessions;
    private final FinalizeSessionUseCase finalizeSession;
    private final Clock clock;

    public ExpireSessionsUseCase(
            SessionRepository sessions, FinalizeSessionUseCase finalizeSession, Clock clock) {
        this.sessions = sessions;
        this.finalizeSession = finalizeSession;
        this.clock = clock;
    }

    public ExpiryRound execute() {
        Instant now = clock.now();
        int expired = sessions.expireDue(now.minus(Session.GRACE));
        List<Session> closed = new ArrayList<>();
        for (UUID id : sessions.findSubmittedIds()) {
            finalizeSession.execute(id).ifPresent(closed::add);
        }
        return new ExpiryRound(expired, List.copyOf(closed));
    }
}
