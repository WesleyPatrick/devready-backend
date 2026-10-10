package br.com.devready.application.evaluation.fake;

import br.com.devready.application.evaluation.exception.XpAlreadyGrantedException;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemorySubmissionRepository implements SubmissionRepository {

    private final Map<UUID, CodeSubmission> submissions = new LinkedHashMap<>();
    private final List<CodeSubmission> saves = new ArrayList<>();
    private int rejectedSaves;
    private Runnable beforeNextFinish = () -> {};
    private Runnable beforeNextStart = () -> {};
    private RuntimeException nextFindBySessionError;

    @Override
    public CodeSubmission save(CodeSubmission submission) {
        if (submission.status() == SubmissionStatus.RUNNING) {
            Runnable action = beforeNextStart;
            beforeNextStart = () -> {};
            action.run();
        }
        if (submission.status().finished()) {
            Runnable action = beforeNextFinish;
            beforeNextFinish = () -> {};
            action.run();
        }
        CodeSubmission current = submissions.get(submission.id());
        if (current != null && current.status().finished()) {
            return current;
        }
        boolean xpTaken =
                submission.xpGained() > 0
                        && submissions.values().stream()
                                .anyMatch(
                                        other ->
                                                !other.id().equals(submission.id())
                                                        && other.userId()
                                                                .equals(submission.userId())
                                                        && other.challengeId()
                                                                .equals(submission.challengeId())
                                                        && other.xpGained() > 0);
        if (xpTaken) {
            rejectedSaves++;
            throw new XpAlreadyGrantedException();
        }
        submissions.put(submission.id(), submission);
        saves.add(submission);
        return submission;
    }

    @Override
    public Optional<CodeSubmission> findById(UUID id) {
        return Optional.ofNullable(submissions.get(id));
    }

    @Override
    public boolean hasXpGained(UUID userId, UUID challengeId) {
        return submissions.values().stream()
                .anyMatch(
                        s ->
                                s.userId().equals(userId)
                                        && s.challengeId().equals(challengeId)
                                        && s.xpGained() > 0);
    }

    @Override
    public List<CodeSubmission> findByUserAndChallenge(
            UUID userId, UUID challengeId, SubmissionKind kind) {
        return submissions.values().stream()
                .filter(
                        s ->
                                s.userId().equals(userId)
                                        && s.challengeId().equals(challengeId)
                                        && s.kind() == kind
                                        && s.sessionId() == null)
                .sorted(
                        Comparator.comparing(CodeSubmission::createdAt)
                                .thenComparing(CodeSubmission::id)
                                .reversed())
                .toList();
    }

    public void failNextFindBySession(RuntimeException error) {
        this.nextFindBySessionError = error;
    }

    @Override
    public List<CodeSubmission> findBySession(UUID sessionId) {
        if (nextFindBySessionError != null) {
            RuntimeException error = nextFindBySessionError;
            nextFindBySessionError = null;
            throw error;
        }
        return submissions.values().stream()
                .filter(s -> sessionId.equals(s.sessionId()))
                .sorted(
                        Comparator.comparing(CodeSubmission::createdAt)
                                .thenComparing(CodeSubmission::id))
                .toList();
    }

    public int rejectedSaves() {
        return rejectedSaves;
    }

    public List<CodeSubmission> all() {
        return List.copyOf(submissions.values());
    }

    public List<CodeSubmission> saves() {
        return List.copyOf(saves);
    }

    public void beforeNextFinish(Runnable action) {
        this.beforeNextFinish = action;
    }

    public void beforeNextStart(Runnable action) {
        this.beforeNextStart = action;
    }
}
