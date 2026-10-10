package br.com.devready.application.practice.usecase;

import static br.com.devready.application.evaluation.fake.SubmissionFixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.evaluation.fake.RecordingSubmissionQueue;
import br.com.devready.application.evaluation.fake.SubmissionFixtures;
import br.com.devready.application.practice.exception.ChallengeInExamException;
import br.com.devready.application.practice.exception.ChallengeNotFoundException;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListChallengeSubmissionsUseCaseTest {

    private final PracticeWorld world = new PracticeWorld();
    private final FixedClock clock = new FixedClock(T0.plusSeconds(100));
    private final ListChallengeSubmissionsUseCase list =
            new ListChallengeSubmissionsUseCase(
                    new SubmissionDesk(
                            world.challenges,
                            world.submissions,
                            new RecordingSubmissionQueue(),
                            new SequentialIdGenerator(),
                            clock),
                    world.submissions,
                    world.examLocks,
                    world.levels);

    private UUID challengeId() {
        return world.challenges.get("c-sql").id();
    }

    @Test
    void listsTheSubmitsOfThePersonNewestFirstWithoutRunsOrOtherPeople() {
        CodeSubmission old =
                world.submissions.save(
                        SubmissionFixtures.finished(
                                world.user, challengeId(), SubmissionStatus.FAILED, 0, T0));
        CodeSubmission recent =
                world.submissions.save(
                        SubmissionFixtures.finished(
                                world.user,
                                challengeId(),
                                SubmissionStatus.PASSED,
                                40,
                                T0.plusSeconds(60)));
        world.submissions.save(
                SubmissionFixtures.run(
                        world.user, challengeId(), SubmissionStatus.PASSED, T0.plusSeconds(90)));
        world.submissions.save(
                SubmissionFixtures.finished(
                        UUID.randomUUID(),
                        challengeId(),
                        SubmissionStatus.PASSED,
                        40,
                        T0.plusSeconds(30)));

        assertThat(list.execute(world.user, "c-sql"))
                .extracting(CodeSubmission::id)
                .containsExactly(recent.id(), old.id());
    }

    @Test
    void theStuckOnesComeOutAsErrorsAndAreSaved() {
        CodeSubmission stuck =
                world.submissions.save(
                        SubmissionFixtures.queued(
                                world.user,
                                world.challenges.get("c-sql"),
                                SubmissionKind.SUBMIT,
                                40,
                                T0));
        clock.advanceTo(T0.plus(Duration.ofMinutes(3)));

        assertThat(list.execute(world.user, "c-sql"))
                .extracting(CodeSubmission::status)
                .containsExactly(SubmissionStatus.ERROR);
        assertThat(world.submissions.findById(stuck.id()).orElseThrow().status())
                .isEqualTo(SubmissionStatus.ERROR);
    }

    @Test
    void anUnknownChallengeIsNotFound() {
        assertThatThrownBy(() -> list.execute(world.user, "nao-existe"))
                .isInstanceOf(ChallengeNotFoundException.class);
    }

    @Test
    void aboveTheLevelOfTheGoalItIsTheSameNotFound() {
        world.withLevels();
        world.withGoal(Level.SENIOR);
        world.submissions.save(
                SubmissionFixtures.finished(
                        world.user,
                        world.challenges.get("c-sql-senior").id(),
                        SubmissionStatus.PASSED,
                        80,
                        T0));
        assertThat(list.execute(world.user, "c-sql-senior")).hasSize(1);

        world.withGoal(Level.JUNIOR);

        assertThatThrownBy(() -> list.execute(world.user, "c-sql-senior"))
                .isInstanceOf(ChallengeNotFoundException.class);
    }

    @Test
    void aChallengeOfAnOpenSimulationIsLocked() {
        world.submissions.save(
                SubmissionFixtures.finished(
                        world.user, challengeId(), SubmissionStatus.PASSED, 40, T0));
        world.examLocks.lock(world.user, challengeId(), UUID.randomUUID());

        assertThatThrownBy(() -> list.execute(world.user, "c-sql"))
                .isInstanceOf(ChallengeInExamException.class);
    }

    @Test
    void sessionSubmissionsStayOutOfTheList() {
        world.submissions.save(
                SubmissionFixtures.finishedInSession(
                        world.user,
                        challengeId(),
                        SubmissionStatus.PASSED,
                        4,
                        UUID.randomUUID(),
                        T0));

        assertThat(list.execute(world.user, "c-sql")).isEmpty();
    }
}
