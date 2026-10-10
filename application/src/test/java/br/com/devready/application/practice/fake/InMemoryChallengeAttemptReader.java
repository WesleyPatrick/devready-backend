package br.com.devready.application.practice.fake;

import br.com.devready.application.evaluation.fake.InMemorySubmissionRepository;
import br.com.devready.application.practice.port.ChallengeAttemptReader;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.practice.ChallengeAttempt;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class InMemoryChallengeAttemptReader implements ChallengeAttemptReader {

    private final InMemorySubmissionRepository submissions;
    private Collection<UUID> lastChallengeQuery = List.of();

    public InMemoryChallengeAttemptReader(InMemorySubmissionRepository submissions) {
        this.submissions = submissions;
    }

    @Override
    public List<ChallengeAttempt> findByUser(UUID userId) {
        return attempts(s -> s.userId().equals(userId));
    }

    @Override
    public List<ChallengeAttempt> findByChallenges(Collection<UUID> challengeIds) {
        lastChallengeQuery = List.copyOf(challengeIds);
        return attempts(s -> challengeIds.contains(s.challengeId()));
    }

    public Collection<UUID> lastChallengeQuery() {
        return lastChallengeQuery;
    }

    private List<ChallengeAttempt> attempts(Predicate<CodeSubmission> filter) {
        return submissions.all().stream()
                .filter(s -> s.kind() == SubmissionKind.SUBMIT)
                .filter(s -> s.sessionId() == null)
                .filter(filter)
                .sorted(Comparator.comparing(CodeSubmission::createdAt))
                .map(
                        s ->
                                new ChallengeAttempt(
                                        s.userId(),
                                        s.challengeId(),
                                        s.status(),
                                        s.xpGained(),
                                        s.createdAt()))
                .toList();
    }
}
