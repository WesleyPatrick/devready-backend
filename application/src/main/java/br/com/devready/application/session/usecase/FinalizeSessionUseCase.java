package br.com.devready.application.session.usecase;

import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.shared.Clock;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.session.BestSubmission;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionChallenge;
import br.com.devready.domain.session.SessionFormat;
import br.com.devready.domain.session.SessionStatus;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class FinalizeSessionUseCase {

    static final String FAILURE = "Falha ao corrigir a sessão: ";

    private final SessionRepository sessions;
    private final SubmissionRepository submissions;
    private final SubmissionDesk desk;
    private final Clock clock;

    public FinalizeSessionUseCase(
            SessionRepository sessions,
            SubmissionRepository submissions,
            SubmissionDesk desk,
            Clock clock) {
        this.sessions = sessions;
        this.submissions = submissions;
        this.desk = desk;
        this.clock = clock;
    }

    public Optional<Session> execute(UUID sessionId) {
        Session session = sessions.findById(sessionId).orElse(null);
        if (session == null || session.status() != SessionStatus.SUBMITTED) {
            return Optional.empty();
        }
        Instant now = clock.now();
        Session closed;
        try {
            Optional<Map<Integer, CodeSubmission>> best = bestSubmissions(session);
            if (best.isEmpty()) {
                return Optional.empty();
            }
            closed = session.complete(best.get(), now);
        } catch (RuntimeException e) {
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            Session failed = session.fail(FAILURE + reason, now);
            return sessions.fail(failed) ? Optional.of(failed) : Optional.empty();
        }
        return sessions.complete(closed) ? Optional.of(closed) : Optional.empty();
    }

    private Optional<Map<Integer, CodeSubmission>> bestSubmissions(Session session) {
        Map<Integer, CodeSubmission> best = new HashMap<>();
        if (session.format() == SessionFormat.LESSON) {
            return Optional.of(best);
        }
        List<CodeSubmission> mine =
                submissions.findBySession(session.id()).stream()
                        .filter(s -> s.kind() == SubmissionKind.SUBMIT)
                        .map(desk::current)
                        .toList();
        if (mine.stream().anyMatch(s -> !s.status().finished())) {
            return Optional.empty();
        }
        for (SessionChallenge challenge : session.challenges()) {
            BestSubmission.of(
                            mine.stream()
                                    .filter(s -> s.challengeId().equals(challenge.challengeId()))
                                    .toList())
                    .ifPresent(submission -> best.put(challenge.position(), submission));
        }
        return Optional.of(best);
    }
}
