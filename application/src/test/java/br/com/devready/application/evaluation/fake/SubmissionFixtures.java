package br.com.devready.application.evaluation.fake;

import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class SubmissionFixtures {

    public static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    private SubmissionFixtures() {}

    public static CodeSubmission queued(
            UUID user, Challenge challenge, SubmissionKind kind, int xpReward, Instant at) {
        boolean run = kind == SubmissionKind.RUN;
        return CodeSubmission.queue(
                UUID.randomUUID(),
                user,
                challenge.id(),
                Language.JAVA,
                "class Solution {}",
                kind,
                run ? challenge.visibleTestCases().size() : challenge.testCases().size(),
                run ? 0 : xpReward,
                at);
    }

    public static CodeSubmission finished(
            UUID user, UUID challengeId, SubmissionStatus status, int xpGained, Instant at) {
        return new CodeSubmission(
                UUID.randomUUID(),
                user,
                challengeId,
                Language.JAVA,
                "class Solution {}",
                SubmissionKind.SUBMIT,
                status,
                status == SubmissionStatus.PASSED ? 4 : 0,
                4,
                null,
                null,
                xpGained > 0 ? xpGained : 40,
                xpGained,
                at,
                status.finished() ? at : null,
                List.of(),
                null);
    }

    public static CodeSubmission finishedInSession(
            UUID user,
            UUID challengeId,
            SubmissionStatus status,
            int passed,
            UUID sessionId,
            Instant at) {
        return new CodeSubmission(
                UUID.randomUUID(),
                user,
                challengeId,
                Language.JAVA,
                "class Solution {}",
                SubmissionKind.SUBMIT,
                status,
                passed,
                4,
                null,
                null,
                0,
                0,
                at,
                status.finished() ? at : null,
                List.of(),
                sessionId);
    }

    public static CodeSubmission run(
            UUID user, UUID challengeId, SubmissionStatus status, Instant at) {
        return new CodeSubmission(
                UUID.randomUUID(),
                user,
                challengeId,
                Language.JAVA,
                "class Solution {}",
                SubmissionKind.RUN,
                status,
                status == SubmissionStatus.PASSED ? 2 : 0,
                2,
                null,
                null,
                0,
                0,
                at,
                status.finished() ? at : null,
                List.of(),
                null);
    }
}
