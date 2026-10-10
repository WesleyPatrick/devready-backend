package br.com.devready.domain.session;

import static br.com.devready.domain.session.SessionFixtures.AREA;
import static br.com.devready.domain.session.SessionFixtures.CONFIG;
import static br.com.devready.domain.session.SessionFixtures.T0;
import static br.com.devready.domain.session.SessionFixtures.USER;
import static br.com.devready.domain.session.SessionFixtures.id;
import static br.com.devready.domain.session.SessionFixtures.lesson;
import static br.com.devready.domain.session.SessionFixtures.simulation;
import static br.com.devready.domain.session.SessionFixtures.submit;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SessionTest {

    private final Instant deadline = T0.plus(Duration.ofMinutes(45));

    @Test
    void aSimulationStartsWithTheDeadlineAndTheWeightOfTheArea() {
        Session session = simulation();

        assertThat(session.format()).isEqualTo(SessionFormat.SIMULATION);
        assertThat(session.status()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(session.deadlineAt()).isEqualTo(deadline);
        assertThat(session.objectiveWeight()).isEqualTo(0.5);
        assertThat(session.competencySlug()).isNull();
        assertThat(session.questions()).extracting(SessionQuestion::position).containsExactly(1, 2);
        assertThat(session.questions()).noneMatch(SessionQuestion::answered);
        assertThat(session.challenges()).extracting(SessionChallenge::position).containsExactly(1);
        assertThat(session.challenges().get(0).submissionsLeft(5)).isEqualTo(5);
        assertThat(session.xpAwarded()).isZero();
        assertThat(session.finishedAt()).isNull();
    }

    @Test
    void aSimulationNeedsExactlyTheCountsOfTheAreaWithoutRepetition() {
        assertThatThrownBy(
                        () ->
                                Session.simulation(
                                        id(1),
                                        USER,
                                        AREA,
                                        Level.JUNIOR,
                                        CONFIG,
                                        List.of(id(1)),
                                        List.of(id(11)),
                                        T0))
                .isInstanceOf(SessionException.class);
        assertThatThrownBy(
                        () ->
                                Session.simulation(
                                        id(1),
                                        USER,
                                        AREA,
                                        Level.JUNIOR,
                                        CONFIG,
                                        List.of(id(1), id(1)),
                                        List.of(id(11)),
                                        T0))
                .isInstanceOf(SessionException.class);
    }

    @Test
    void aLessonHasNoDeadlineNoChallengesAndThreeToEightQuestions() {
        Session session = lesson();

        assertThat(session.format()).isEqualTo(SessionFormat.LESSON);
        assertThat(session.deadlineAt()).isNull();
        assertThat(session.objectiveWeight()).isNull();
        assertThat(session.competencySlug()).isEqualTo("sql");
        assertThat(session.challenges()).isEmpty();
        assertThatThrownBy(
                        () ->
                                Session.lesson(
                                        id(1),
                                        USER,
                                        AREA,
                                        Level.JUNIOR,
                                        "sql",
                                        List.of(id(1), id(2)),
                                        T0))
                .isInstanceOf(SessionException.class);
        List<java.util.UUID> nine =
                List.of(id(1), id(2), id(3), id(4), id(5), id(6), id(7), id(8), id(9));
        assertThatThrownBy(() -> Session.lesson(id(1), USER, AREA, Level.JUNIOR, "sql", nine, T0))
                .isInstanceOf(SessionException.class);
    }

    @Test
    void answersAndSubmissionsAreAcceptedUntilThirtySecondsAfterTheDeadline() {
        Session session = simulation();

        assertThat(session.acceptsAt(deadline.plusSeconds(30))).isTrue();
        assertThat(session.acceptsAt(deadline.plusSeconds(31))).isFalse();
        assertThat(session.submit().acceptsAt(T0)).isFalse();
        assertThat(lesson().acceptsAt(T0.plus(Duration.ofDays(30)))).isTrue();
        assertThatThrownBy(() -> session.answer(1, id(50), true, deadline.plusSeconds(31)))
                .isInstanceOf(SessionException.class);
    }

    @Test
    void inTheSimulationTheAnswerChangesUntilTheEndAndInTheLessonItIsFinal() {
        Session answered = simulation().answer(1, id(50), false, T0).answer(1, id(51), true, T0);

        assertThat(answered.questions().get(0).optionId()).isEqualTo(id(51));
        assertThat(answered.questions().get(0).hit()).isTrue();

        Session lesson = lesson().answer(2, id(50), true, T0);
        assertThat(lesson.questions().get(1).answered()).isTrue();
        assertThatThrownBy(() -> lesson.answer(2, id(51), false, T0))
                .isInstanceOf(SessionException.class);
        assertThatThrownBy(() -> lesson.answer(4, id(51), false, T0))
                .isInstanceOf(SessionException.class);
        assertThat(lesson.allAnswered()).isFalse();
        assertThat(lesson.answer(1, id(52), false, T0).answer(3, id(53), true, T0).allAnswered())
                .isTrue();
    }

    @Test
    void eachChallengeTakesAsManySubmissionsAsTheLimitOfTheChallenge() {
        Session session = simulation();
        for (int i = 0; i < 3; i++) {
            session = session.useSubmission(1, 3, T0);
        }

        assertThat(session.challenges().get(0).submissionsUsed()).isEqualTo(3);
        assertThat(session.challenges().get(0).submissionsLeft(3)).isZero();
        assertThat(session.challenges().get(0).submissionsLeft(5)).isEqualTo(2);
        Session spent = session;
        assertThatThrownBy(() -> spent.useSubmission(1, 3, T0))
                .isInstanceOf(SessionException.class);
        assertThat(spent.useSubmission(1, 5, T0).challenges().get(0).submissionsUsed())
                .isEqualTo(4);
        assertThatThrownBy(() -> simulation().useSubmission(2, 5, T0))
                .isInstanceOf(SessionException.class);
        assertThatThrownBy(() -> lesson().useSubmission(1, 5, T0))
                .isInstanceOf(SessionException.class);
    }

    @Test
    void submittingTwiceChangesNothing() {
        Session submitted = simulation().submit();

        assertThat(submitted.status()).isEqualTo(SessionStatus.SUBMITTED);
        assertThat(submitted.submit()).isEqualTo(submitted);
        Session completed = submitted.complete(Map.of(), deadline);
        assertThat(completed.submit()).isEqualTo(completed);
    }

    @Test
    void theCorrectionComputesTheScoreTheBonusAndKeepsTheBestSubmission() {
        CodeSubmission best = submit(id(11), SubmissionStatus.FAILED, 3, T0.plusSeconds(60));
        Session session =
                simulation()
                        .answer(1, id(50), true, T0)
                        .answer(2, id(51), false, T0)
                        .submit()
                        .complete(Map.of(1, best), deadline);

        assertThat(session.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(session.finishedAt()).isEqualTo(deadline);
        assertThat(session.objectiveScore()).isEqualTo(0.5);
        assertThat(session.challengeScore()).isEqualTo(0.75);
        assertThat(session.finalScore()).isEqualTo(0.625);
        assertThat(session.xpAwarded()).isEqualTo(63);
        assertThat(session.challenges().get(0).bestSubmissionId()).isEqualTo(best.id());
    }

    @Test
    void aSimulationWithoutAnswersOrSubmissionsGivesNoBonus() {
        Session empty = simulation().submit().complete(Map.of(), deadline);

        assertThat(empty.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(empty.finalScore()).isZero();
        assertThat(empty.xpAwarded()).isZero();
        assertThat(
                        simulation()
                                .answer(1, id(51), false, T0)
                                .submit()
                                .complete(Map.of(), deadline)
                                .xpAwarded())
                .isEqualTo(SessionXp.MIN);
        assertThat(
                        simulation()
                                .useSubmission(1, 5, T0)
                                .submit()
                                .complete(Map.of(), deadline)
                                .xpAwarded())
                .isEqualTo(SessionXp.MIN);
        CodeSubmission zero = submit(id(11), SubmissionStatus.FAILED, 0, T0.plusSeconds(60));
        assertThat(simulation().submit().complete(Map.of(1, zero), deadline).xpAwarded())
                .isEqualTo(SessionXp.MIN);
    }

    @Test
    void aBestSubmissionOfAnotherChallengeIsABug() {
        CodeSubmission other = submit(id(99), SubmissionStatus.PASSED, 4, T0);

        assertThatThrownBy(() -> simulation().submit().complete(Map.of(1, other), deadline))
                .isInstanceOf(SessionException.class);
    }

    @Test
    void theLessonIsCorrectedWithOnlyTheObjectiveScoreAndNoBonus() {
        Session session =
                lesson().answer(1, id(50), true, T0)
                        .answer(2, id(51), true, T0)
                        .submit()
                        .complete(Map.of(), T0.plusSeconds(90));

        assertThat(session.objectiveScore()).isEqualTo(0.6667);
        assertThat(session.challengeScore()).isNull();
        assertThat(session.finalScore()).isNull();
        assertThat(session.xpAwarded()).isZero();
    }

    @Test
    void onlyASubmittedSessionIsCorrectedOrFails() {
        assertThatThrownBy(() -> simulation().complete(Map.of(), T0))
                .isInstanceOf(SessionException.class);
        assertThatThrownBy(() -> simulation().fail("motivo", T0))
                .isInstanceOf(SessionException.class);

        Session failed = simulation().submit().fail("Desafio sumiu do catálogo.", deadline);
        assertThat(failed.status()).isEqualTo(SessionStatus.FAILED);
        assertThat(failed.failureReason()).isEqualTo("Desafio sumiu do catálogo.");
        assertThat(failed.finishedAt()).isEqualTo(deadline);
        assertThat(failed.objectiveScore()).isNull();
    }

    @Test
    void theLessonRevealsEachAnsweredQuestionAndTheSimulationOnlyAfterTheCorrection() {
        Session lesson = lesson().answer(1, id(50), true, T0);
        assertThat(lesson.reveals(lesson.questions().get(0))).isTrue();
        assertThat(lesson.reveals(lesson.questions().get(1))).isFalse();

        Session simulation = simulation().answer(1, id(50), true, T0);
        assertThat(simulation.reveals(simulation.questions().get(0))).isFalse();
        Session corrected = simulation.submit().complete(Map.of(), deadline);
        assertThat(corrected.reveals(corrected.questions().get(1))).isTrue();
    }
}
