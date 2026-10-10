package br.com.devready.infrastructure.session.persistence;

import br.com.devready.application.session.exception.OpenSessionConflictException;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionChallenge;
import br.com.devready.domain.session.SessionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaSessionRepository implements SessionRepository {

    static final String ONE_IN_PROGRESS_INDEX = "sessions_one_in_progress_idx";

    private final SpringDataSessionRepository sessions;

    public JpaSessionRepository(SpringDataSessionRepository sessions) {
        this.sessions = sessions;
    }

    @Override
    public Session create(Session session) {
        try {
            sessions.saveAndFlush(SessionEntity.from(session));
        } catch (DataIntegrityViolationException e) {
            if (mentions(e, ONE_IN_PROGRESS_INDEX)) {
                throw new OpenSessionConflictException();
            }
            throw e;
        }
        return session;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Session> findById(UUID id) {
        return sessions.findById(id).map(SessionEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Session> findOpen(UUID userId) {
        return sessions.findFirstByUserIdAndStatus(userId, SessionStatus.IN_PROGRESS)
                .map(SessionEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Session> findByUser(UUID userId) {
        return sessions.findByUserIdOrderByStartedAtDescIdDesc(userId).stream()
                .map(SessionEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UUID> findSubmittedIds() {
        return sessions.idsWithStatus(SessionStatus.SUBMITTED);
    }

    @Override
    public boolean answer(
            UUID sessionId, int position, UUID optionId, boolean correct, boolean replace) {
        return sessions.answer(sessionId, position, optionId, correct, replace) == 1;
    }

    @Override
    public boolean useSubmission(UUID sessionId, int position, int maxSubmissions) {
        return sessions.useSubmission(sessionId, position, maxSubmissions) == 1;
    }

    @Override
    public boolean submit(UUID sessionId) {
        return sessions.changeStatus(sessionId, SessionStatus.IN_PROGRESS, SessionStatus.SUBMITTED)
                == 1;
    }

    @Override
    public int expireDue(Instant cutoff) {
        return sessions.expireDue(SessionStatus.IN_PROGRESS, SessionStatus.SUBMITTED, cutoff);
    }

    @Override
    public boolean complete(Session completed) {
        int changed =
                sessions.complete(
                        completed.id(),
                        SessionStatus.SUBMITTED,
                        SessionStatus.COMPLETED,
                        completed.finishedAt(),
                        completed.objectiveScore(),
                        completed.challengeScore(),
                        completed.finalScore(),
                        completed.xpAwarded());
        if (changed == 0) {
            return false;
        }
        for (SessionChallenge challenge : completed.challenges()) {
            if (challenge.bestSubmissionId() != null) {
                sessions.setBest(
                        completed.id(), challenge.position(), challenge.bestSubmissionId());
            }
        }
        return true;
    }

    @Override
    public boolean fail(Session failed) {
        return sessions.fail(
                        failed.id(),
                        SessionStatus.SUBMITTED,
                        SessionStatus.FAILED,
                        failed.failureReason(),
                        failed.finishedAt())
                == 1;
    }

    private static boolean mentions(Throwable error, String constraint) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }
}
