package br.com.devready.application.practice.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.practice.DailyGoal;
import br.com.devready.domain.practice.PracticeCalendar;
import br.com.devready.domain.practice.PracticeProgress;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetProgressUseCaseTest {

    private static final LocalDate DAY = LocalDate.ofEpochDay(20_000);

    private final PracticeWorld world = new PracticeWorld();
    private final FixedClock clock = new FixedClock(at(0, 12, 0));
    private final GetProgressUseCase getProgress =
            new GetProgressUseCase(world.answers, world.attempts, world.simulations, clock);

    private static Instant at(int days, int hour, int minute) {
        return ZonedDateTime.of(
                        DAY.plusDays(days), LocalTime.of(hour, minute), PracticeCalendar.ZONE)
                .toInstant();
    }

    @Test
    void countsTheAnswersOfThePersonByTheDayInBrasilia() {
        world.answered(world.user, "q-java", false, 0, at(-1, 23, 59));
        world.answered(world.user, "q-java", true, 20, at(0, 0, 0));
        world.answered(world.user, "q-http", true, 20, at(0, 9, 0));
        world.answered(UUID.randomUUID(), "q-http", true, 20, at(0, 9, 0));

        PracticeProgress progress = getProgress.execute(world.user);

        assertThat(progress.xp()).isEqualTo(40);
        assertThat(progress.streakDays()).isEqualTo(2);
        assertThat(progress.dailyGoal()).isEqualTo(new DailyGoal(2, 3));
        assertThat(progress.activity()).hasSize(35);
        assertThat(progress.activity().get(34).date()).isEqualTo(DAY);
        assertThat(progress.activity().get(34).count()).isEqualTo(2);
        assertThat(progress.activity().get(33).count()).isEqualTo(1);
    }

    @Test
    void withoutAnswersEverythingIsZero() {
        PracticeProgress progress = getProgress.execute(world.user);

        assertThat(progress.xp()).isZero();
        assertThat(progress.streakDays()).isZero();
        assertThat(progress.dailyGoal()).isEqualTo(new DailyGoal(0, 3));
    }

    @Test
    void submissionsOfThePersonCountLikeAnswers() {
        world.answered(world.user, "q-java", true, 20, at(0, 9, 0));
        world.submitted(world.user, "c-sql", SubmissionStatus.PASSED, 40, at(0, 10, 0));
        world.submitted(world.user, "c-sql", SubmissionStatus.ERROR, 0, at(0, 11, 0));
        world.submitted(UUID.randomUUID(), "c-sql", SubmissionStatus.PASSED, 40, at(0, 10, 0));

        PracticeProgress progress = getProgress.execute(world.user);

        assertThat(progress.xp()).isEqualTo(60);
        assertThat(progress.dailyGoal()).isEqualTo(new DailyGoal(2, 3));
        assertThat(progress.activity().get(34).count()).isEqualTo(2);
    }

    @Test
    void aCorrectedSimulationIsOneActivityWithItsBonusAndSessionSubmissionsDoNotCount() {
        world.simulations.complete(world.user, at(0, 10, 0), 63);
        world.submittedInSession(
                world.user, "c-sql", SubmissionStatus.PASSED, 4, UUID.randomUUID(), at(0, 9, 0));

        PracticeProgress progress = getProgress.execute(world.user);

        assertThat(progress.xp()).isEqualTo(63);
        assertThat(progress.dailyGoal()).isEqualTo(new DailyGoal(1, 3));
        assertThat(progress.streakDays()).isEqualTo(1);
    }
}
