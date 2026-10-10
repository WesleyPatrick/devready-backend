package br.com.devready.application.session.fake;

import br.com.devready.application.session.exception.OpenSessionConflictException;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionQuestion;
import br.com.devready.domain.session.SessionStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemorySessionRepository implements SessionRepository {

    private final Map<UUID, Session> sessions = new LinkedHashMap<>();
    private Runnable beforeNextCreate = () -> {};
    private Runnable afterNextAnswer = () -> {};

    @Override
    public Session create(Session session) {
        Runnable action = beforeNextCreate;
        beforeNextCreate = () -> {};
        action.run();
        if (session.status() == SessionStatus.IN_PROGRESS
                && findOpen(session.userId()).isPresent()) {
            throw new OpenSessionConflictException();
        }
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public Optional<Session> findById(UUID id) {
        return Optional.ofNullable(sessions.get(id));
    }

    @Override
    public Optional<Session> findOpen(UUID userId) {
        return sessions.values().stream()
                .filter(s -> s.userId().equals(userId) && s.status() == SessionStatus.IN_PROGRESS)
                .findFirst();
    }

    @Override
    public List<Session> findByUser(UUID userId) {
        return sessions.values().stream()
                .filter(s -> s.userId().equals(userId))
                .sorted(
                        Comparator.comparing(Session::startedAt)
                                .thenComparing(Session::id)
                                .reversed())
                .toList();
    }

    @Override
    public List<UUID> findSubmittedIds() {
        return sessions.values().stream()
                .filter(s -> s.status() == SessionStatus.SUBMITTED)
                .map(Session::id)
                .toList();
    }

    @Override
    public boolean answer(
            UUID sessionId, int position, UUID optionId, boolean correct, boolean replace) {
        Session session = sessions.get(sessionId);
        if (session == null || session.status() != SessionStatus.IN_PROGRESS) {
            return false;
        }
        Optional<SessionQuestion> question = session.question(position);
        if (question.isEmpty() || (!replace && question.get().answered())) {
            return false;
        }
        sessions.put(sessionId, session.answer(position, optionId, correct, session.startedAt()));
        Runnable action = afterNextAnswer;
        afterNextAnswer = () -> {};
        action.run();
        return true;
    }

    @Override
    public boolean useSubmission(UUID sessionId, int position, int maxSubmissions) {
        Session session = sessions.get(sessionId);
        if (session == null
                || session.status() != SessionStatus.IN_PROGRESS
                || session.challenge(position)
                        .map(c -> c.submissionsLeft(maxSubmissions) == 0)
                        .orElse(true)) {
            return false;
        }
        sessions.put(
                sessionId, session.useSubmission(position, maxSubmissions, session.startedAt()));
        return true;
    }

    @Override
    public boolean submit(UUID sessionId) {
        Session session = sessions.get(sessionId);
        if (session == null || session.status() != SessionStatus.IN_PROGRESS) {
            return false;
        }
        sessions.put(sessionId, session.submit());
        return true;
    }

    @Override
    public int expireDue(Instant cutoff) {
        List<Session> due =
                sessions.values().stream()
                        .filter(
                                s ->
                                        s.status() == SessionStatus.IN_PROGRESS
                                                && s.deadlineAt() != null
                                                && s.deadlineAt().isBefore(cutoff))
                        .toList();
        due.forEach(s -> sessions.put(s.id(), s.submit()));
        return due.size();
    }

    @Override
    public boolean complete(Session completed) {
        return replaceSubmitted(completed);
    }

    @Override
    public boolean fail(Session failed) {
        return replaceSubmitted(failed);
    }

    private boolean replaceSubmitted(Session closed) {
        Session current = sessions.get(closed.id());
        if (current == null || current.status() != SessionStatus.SUBMITTED) {
            return false;
        }
        sessions.put(closed.id(), closed);
        return true;
    }

    public void beforeNextCreate(Runnable action) {
        this.beforeNextCreate = action;
    }

    public void afterNextAnswer(Runnable action) {
        this.afterNextAnswer = action;
    }

    public void put(Session session) {
        sessions.put(session.id(), session);
    }

    public Session get(UUID id) {
        return sessions.get(id);
    }
}
