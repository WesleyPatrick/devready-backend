package br.com.devready.application.session.usecase;

import br.com.devready.application.practice.dto.SubmissionTicket;
import br.com.devready.application.practice.exception.LanguageNotOfferedException;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.session.exception.ItemNotInSessionException;
import br.com.devready.application.session.exception.NoSubmissionsLeftException;
import br.com.devready.application.session.exception.SessionClosedException;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.session.service.SessionAccess;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.Transactions;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionChallenge;
import br.com.devready.domain.session.SessionStatus;
import java.time.Instant;
import java.util.UUID;

public class SubmitSessionCodeUseCase {

    private final SessionRepository sessions;
    private final SessionPool pool;
    private final SubmissionDesk desk;
    private final Transactions transactions;
    private final Clock clock;

    public SubmitSessionCodeUseCase(
            SessionRepository sessions,
            SessionPool pool,
            SubmissionDesk desk,
            Transactions transactions,
            Clock clock) {
        this.sessions = sessions;
        this.pool = pool;
        this.desk = desk;
        this.transactions = transactions;
        this.clock = clock;
    }

    public SubmissionTicket execute(
            UUID userId, UUID sessionId, int position, Language language, String sourceCode) {
        Session session = SessionAccess.owned(sessions, userId, sessionId);
        SessionChallenge item =
                session.challenge(position)
                        .orElseThrow(() -> new ItemNotInSessionException("desafio", position));
        Instant now = clock.now();
        if (!session.acceptsAt(now)) {
            throw new SessionClosedException();
        }
        Challenge challenge = SessionAccess.challengeOf(pool, item);
        int limit = challenge.maxSubmissions();
        if (item.submissionsLeft(limit) == 0) {
            throw new NoSubmissionsLeftException(position, limit);
        }
        if (!challenge.languages().containsKey(language)) {
            throw new LanguageNotOfferedException(language);
        }
        return transactions.inTransaction(
                () -> {
                    if (!sessions.useSubmission(sessionId, position, limit)) {
                        throw sessions.findById(sessionId)
                                        .filter(s -> s.status() == SessionStatus.IN_PROGRESS)
                                        .isPresent()
                                ? new NoSubmissionsLeftException(position, limit)
                                : new SessionClosedException();
                    }
                    return desk.queueInSession(
                            userId,
                            challenge,
                            language,
                            sourceCode,
                            SubmissionKind.SUBMIT,
                            sessionId);
                });
    }
}
