package br.com.devready.application.session.usecase;

import br.com.devready.application.practice.dto.SubmissionTicket;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.session.exception.ItemNotInSessionException;
import br.com.devready.application.session.exception.SessionClosedException;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.session.service.SessionAccess;
import br.com.devready.application.shared.Clock;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionChallenge;
import java.util.UUID;

public class RunSessionCodeUseCase {

    private final SessionRepository sessions;
    private final SessionPool pool;
    private final SubmissionDesk desk;
    private final Clock clock;

    public RunSessionCodeUseCase(
            SessionRepository sessions, SessionPool pool, SubmissionDesk desk, Clock clock) {
        this.sessions = sessions;
        this.pool = pool;
        this.desk = desk;
        this.clock = clock;
    }

    public SubmissionTicket execute(
            UUID userId, UUID sessionId, int position, Language language, String sourceCode) {
        Session session = SessionAccess.owned(sessions, userId, sessionId);
        SessionChallenge item =
                session.challenge(position)
                        .orElseThrow(() -> new ItemNotInSessionException("desafio", position));
        if (!session.acceptsAt(clock.now())) {
            throw new SessionClosedException();
        }
        return desk.queueInSession(
                userId,
                SessionAccess.challengeOf(pool, item),
                language,
                sourceCode,
                SubmissionKind.RUN,
                sessionId);
    }
}
