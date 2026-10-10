package br.com.devready.domain.session;

import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class SessionFixtures {

    static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);
    static final UUID USER = new UUID(0, 1);
    static final UUID AREA = new UUID(0, 2);

    static final SessionConfig CONFIG = new SessionConfig(2, 1, 45, 0.5);

    private SessionFixtures() {}

    static UUID id(long n) {
        return new UUID(9, n);
    }

    static Session simulation() {
        return Session.simulation(
                id(100),
                USER,
                AREA,
                Level.JUNIOR,
                CONFIG,
                List.of(id(1), id(2)),
                List.of(id(11)),
                T0);
    }

    static Session lesson() {
        return Session.lesson(
                id(200), USER, AREA, Level.JUNIOR, "sql", List.of(id(1), id(2), id(3)), T0);
    }

    static CodeSubmission submit(UUID challenge, SubmissionStatus status, int passed, Instant at) {
        return new CodeSubmission(
                UUID.randomUUID(),
                USER,
                challenge,
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
                at,
                List.of(),
                null);
    }

    static CodeSubmission queued(UUID challenge, Instant at) {
        return new CodeSubmission(
                UUID.randomUUID(),
                USER,
                challenge,
                Language.JAVA,
                "class Solution {}",
                SubmissionKind.SUBMIT,
                SubmissionStatus.QUEUED,
                0,
                4,
                null,
                null,
                0,
                0,
                at,
                null,
                List.of(),
                null);
    }

    static CodeSubmission run(UUID challenge, int passed, Instant at) {
        return new CodeSubmission(
                UUID.randomUUID(),
                USER,
                challenge,
                Language.JAVA,
                "class Solution {}",
                SubmissionKind.RUN,
                passed == 2 ? SubmissionStatus.PASSED : SubmissionStatus.FAILED,
                passed,
                2,
                null,
                null,
                0,
                0,
                at,
                at,
                List.of(),
                null);
    }
}
