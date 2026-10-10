package br.com.devready.application.session.usecase;

import static br.com.devready.application.session.fake.SessionWorld.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.evaluation.fake.SubmissionFixtures;
import br.com.devready.application.session.exception.SessionNotFoundException;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionStatus;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FinishSessionUseCaseTest {

    private final SessionWorld world = new SessionWorld();

    private CodeSubmission finished(
            Session session, String challenge, SubmissionStatus status, int passed, int seconds) {
        return world.practice.submittedInSession(
                world.user, challenge, status, passed, session.id(), T0.plusSeconds(seconds));
    }

    @Test
    void withoutSubmissionsRunningTheCorrectionHappensAtOnce() {
        Session session = world.openSimulation();
        world.sessions.answer(
                session.id(),
                world.position(session, "q-java"),
                world.option("q-java", true),
                true,
                true);
        CodeSubmission early = finished(session, "c-sql", SubmissionStatus.FAILED, 3, 10);
        finished(session, "c-sql", SubmissionStatus.FAILED, 3, 20);
        finished(session, "c-sql", SubmissionStatus.FAILED, 1, 30);
        world.clock.advanceTo(T0.plus(Duration.ofMinutes(20)));

        Session result = world.finishSession.execute(world.user, session.id());

        assertThat(result.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(result.finishedAt()).isEqualTo(T0.plus(Duration.ofMinutes(20)));
        assertThat(result.objectiveScore()).isEqualTo(0.125);
        assertThat(result.challengeScore()).isEqualTo(0.375);
        assertThat(result.finalScore()).isEqualTo(0.25);
        assertThat(result.xpAwarded()).isEqualTo(25);
        assertThat(
                        result.challenge(world.challengePosition(session, "c-sql"))
                                .orElseThrow()
                                .bestSubmissionId())
                .isEqualTo(early.id());
        assertThat(
                        result.challenge(world.challengePosition(session, "c-two"))
                                .orElseThrow()
                                .bestSubmissionId())
                .isNull();
    }

    @Test
    void withASubmissionRunningTheSessionWaitsAndTheCorrectionComesLater() {
        Session session = world.openSimulation();
        CodeSubmission pending =
                world.practice.submissions.save(
                        CodeSubmission.queueInSession(
                                UUID.randomUUID(),
                                world.user,
                                world.practice.challenges.get("c-sql").id(),
                                Language.JAVA,
                                "x",
                                SubmissionKind.SUBMIT,
                                4,
                                session.id(),
                                T0));

        assertThat(world.finishSession.execute(world.user, session.id()).status())
                .isEqualTo(SessionStatus.SUBMITTED);
        assertThat(world.finalizeSession.execute(session.id())).isEmpty();

        world.practice.submissions.save(pending.fail("terminou", T0.plusSeconds(5)));
        Optional<Session> corrected = world.finalizeSession.execute(session.id());

        assertThat(corrected).isPresent();
        assertThat(corrected.get().status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(world.finalizeSession.execute(session.id())).isEmpty();
    }

    @Test
    void aStuckSubmissionExpiresAndCountsAsZero() {
        Session session = world.openSimulation();
        world.practice.submissions.save(
                CodeSubmission.queueInSession(
                        UUID.randomUUID(),
                        world.user,
                        world.practice.challenges.get("c-sql").id(),
                        Language.JAVA,
                        "x",
                        SubmissionKind.SUBMIT,
                        4,
                        session.id(),
                        T0));
        world.finishSession.execute(world.user, session.id());
        world.clock.advanceTo(T0.plus(Duration.ofMinutes(3)));

        Session corrected = world.finalizeSession.execute(session.id()).orElseThrow();

        assertThat(corrected.challengeScore()).isZero();
        assertThat(world.practice.submissions.findBySession(session.id()).get(0).status())
                .isEqualTo(SubmissionStatus.ERROR);
    }

    @Test
    void aRunningRunDoesNotHoldTheCorrection() {
        Session session = world.openSimulation();
        world.practice.submissions.save(
                CodeSubmission.queueInSession(
                        UUID.randomUUID(),
                        world.user,
                        world.practice.challenges.get("c-sql").id(),
                        Language.JAVA,
                        "x",
                        SubmissionKind.RUN,
                        2,
                        session.id(),
                        T0));

        assertThat(world.finishSession.execute(world.user, session.id()).status())
                .isEqualTo(SessionStatus.COMPLETED);
    }

    @Test
    void finishingTwiceChangesNothing() {
        Session session = world.openSimulation();
        Session first = world.finishSession.execute(world.user, session.id());
        world.clock.advanceTo(T0.plusSeconds(99));

        Session second = world.finishSession.execute(world.user, session.id());

        assertThat(second).isEqualTo(first);
        assertThatThrownBy(() -> world.finishSession.execute(UUID.randomUUID(), session.id()))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void onlyASubmittedSessionIsCorrected() {
        Session session = world.openSimulation();

        assertThat(world.finalizeSession.execute(session.id())).isEmpty();
        assertThat(world.finalizeSession.execute(UUID.randomUUID())).isEmpty();
        assertThat(world.sessions.get(session.id()).status()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    void aSystemErrorInTheCorrectionFailsTheSessionWithTheReason() {
        Session session = world.openSimulation();
        world.sessions.submit(session.id());
        world.practice.submissions.failNextFindBySession(new IllegalStateException("banco fora"));

        Session failed = world.finalizeSession.execute(session.id()).orElseThrow();

        assertThat(failed.status()).isEqualTo(SessionStatus.FAILED);
        assertThat(failed.failureReason()).isEqualTo("Falha ao corrigir a sessão: banco fora");
        assertThat(world.sessions.get(session.id()).status()).isEqualTo(SessionStatus.FAILED);
    }

    @Test
    void aSystemErrorWithoutMessageUsesTheClassNameAsTheReason() {
        Session session = world.openSimulation();
        world.sessions.submit(session.id());
        world.practice.submissions.failNextFindBySession(new IllegalStateException());

        Session failed = world.finalizeSession.execute(session.id()).orElseThrow();

        assertThat(failed.failureReason())
                .isEqualTo("Falha ao corrigir a sessão: IllegalStateException");
    }

    @Test
    void aSessionClosedByAnotherCallIsNotCorrectedTwice() {
        Session session = world.openSimulation();
        world.sessions.submit(session.id());
        world.sessions.put(
                world.sessions.get(session.id()).complete(java.util.Map.of(), T0.plusSeconds(1)));

        assertThat(world.finalizeSession.execute(session.id())).isEmpty();
    }

    @Test
    void practiceSubmissionsOfThePersonDoNotEnterTheCorrection() {
        Session session = world.openSimulation();
        world.practice.submissions.save(
                SubmissionFixtures.finished(
                        world.user,
                        world.practice.challenges.get("c-sql").id(),
                        SubmissionStatus.PASSED,
                        40,
                        T0));

        assertThat(world.finishSession.execute(world.user, session.id()).challengeScore()).isZero();
    }
}
