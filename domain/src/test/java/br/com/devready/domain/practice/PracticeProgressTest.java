package br.com.devready.domain.practice;

import static br.com.devready.domain.practice.AnswerFixtures.ANA;
import static br.com.devready.domain.practice.AnswerFixtures.DAY;
import static br.com.devready.domain.practice.AnswerFixtures.answer;
import static br.com.devready.domain.practice.AnswerFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PracticeProgressTest {

    private final UUID question = UUID.randomUUID();
    private final Instant noon = at(0, 12, 0);

    private Answer on(int days) {
        return answer(ANA, question, false, 0, at(days, 10, 0));
    }

    @Test
    void withoutAnswersEverythingIsZeroAndTheCalendarEndsToday() {
        PracticeProgress progress = PracticeProgress.of(List.of(), List.of(), List.of(), noon);

        assertThat(progress.xp()).isZero();
        assertThat(progress.streakDays()).isZero();
        assertThat(progress.dailyGoal()).isEqualTo(new DailyGoal(0, 3));
        assertThat(progress.activity()).hasSize(35);
        assertThat(progress.activity().get(0).date()).isEqualTo(DAY.minusDays(34));
        assertThat(progress.activity().get(34).date()).isEqualTo(DAY);
        assertThat(progress.activity()).allMatch(day -> day.count() == 0);
    }

    @Test
    void addsTheXpOfTheAnswers() {
        PracticeProgress progress =
                PracticeProgress.of(
                        List.of(
                                answer(ANA, question, true, 20, at(-1, 9, 0)),
                                answer(ANA, question, false, 0, at(0, 9, 0)),
                                answer(ANA, UUID.randomUUID(), true, 40, at(0, 9, 5))),
                        List.of(),
                        List.of(),
                        noon);

        assertThat(progress.xp()).isEqualTo(60);
    }

    @Test
    void theDayOfAnAnswerIsTheOneInBrasilia() {
        PracticeProgress progress =
                PracticeProgress.of(
                        List.of(
                                answer(ANA, question, false, 0, at(-1, 23, 59)),
                                answer(ANA, question, false, 0, at(0, 0, 0)),
                                answer(
                                        ANA,
                                        question,
                                        false,
                                        0,
                                        DAY.atTime(2, 30).toInstant(ZoneOffset.UTC))),
                        List.of(),
                        List.of(),
                        noon);

        assertThat(progress.activity().get(33).count()).isEqualTo(2);
        assertThat(progress.activity().get(34).count()).isEqualTo(1);
    }

    @Test
    void theStreakCountsTheDaysInARowUpToToday() {
        PracticeProgress progress =
                PracticeProgress.of(List.of(on(-2), on(-1), on(0)), List.of(), List.of(), noon);

        assertThat(progress.streakDays()).isEqualTo(3);
    }

    @Test
    void withoutAnAnswerTodayTheStreakIsStillAliveUntilYesterday() {
        PracticeProgress progress =
                PracticeProgress.of(List.of(on(-2), on(-1)), List.of(), List.of(), noon);

        assertThat(progress.streakDays()).isEqualTo(2);
    }

    @Test
    void aWholeDayWithoutAnswersResetsTheStreak() {
        assertThat(
                        PracticeProgress.of(List.of(on(-3), on(-2)), List.of(), List.of(), noon)
                                .streakDays())
                .isZero();
        assertThat(
                        PracticeProgress.of(
                                        List.of(on(-3), on(-2), on(0)), List.of(), List.of(), noon)
                                .streakDays())
                .isEqualTo(1);
    }

    @Test
    void theStreakTurnsAtMidnightInBrasilia() {
        List<Answer> lastMinute = List.of(answer(ANA, question, false, 0, at(-1, 23, 59)));

        assertThat(
                        PracticeProgress.of(lastMinute, List.of(), List.of(), at(-1, 23, 59))
                                .streakDays())
                .isEqualTo(1);
        assertThat(PracticeProgress.of(lastMinute, List.of(), List.of(), at(0, 0, 0)).streakDays())
                .isEqualTo(1);
        assertThat(PracticeProgress.of(lastMinute, List.of(), List.of(), at(1, 0, 0)).streakDays())
                .isZero();
    }

    @Test
    void theGoalStopsAtThreeButTheCalendarCountsEveryAnswer() {
        List<Answer> today = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            today.add(answer(ANA, UUID.randomUUID(), false, 0, at(0, 9, i)));
        }

        PracticeProgress progress = PracticeProgress.of(today, List.of(), List.of(), noon);

        assertThat(progress.dailyGoal()).isEqualTo(new DailyGoal(3, 3));
        assertThat(progress.activity().get(34).count()).isEqualTo(5);
    }

    @Test
    void aWrongAnswerThenARightOneBothCountForTheGoal() {
        PracticeProgress progress =
                PracticeProgress.of(
                        List.of(
                                answer(ANA, question, false, 0, at(0, 9, 0)),
                                answer(ANA, question, true, 30, at(0, 9, 1))),
                        List.of(),
                        List.of(),
                        noon);

        assertThat(progress.dailyGoal().done()).isEqualTo(2);
        assertThat(progress.xp()).isEqualTo(30);
    }

    @Test
    void theStreakGoesBeyondTheFiveWeeksOfTheCalendar() {
        List<Answer> everyDay = new ArrayList<>();
        for (int days = -39; days <= 0; days++) {
            everyDay.add(on(days));
        }

        PracticeProgress progress = PracticeProgress.of(everyDay, List.of(), List.of(), noon);

        assertThat(progress.streakDays()).isEqualTo(40);
        assertThat(progress.activity()).hasSize(35).allMatch(day -> day.count() == 1);
    }

    @Test
    void aCompletedSimulationAddsItsBonusAndIsOneActivityOnTheDayItFinished() {
        PracticeProgress progress =
                PracticeProgress.of(
                        List.of(answer(ANA, question, true, 20, at(-1, 9, 0))),
                        List.of(),
                        List.of(
                                new CompletedSimulation(at(0, 0, 0), 63),
                                new CompletedSimulation(at(-1, 23, 59), 10)),
                        noon);

        assertThat(progress.xp()).isEqualTo(93);
        assertThat(progress.streakDays()).isEqualTo(2);
        assertThat(progress.dailyGoal()).isEqualTo(new DailyGoal(1, 3));
        assertThat(progress.activity().get(34).count()).isEqualTo(1);
        assertThat(progress.activity().get(33).count()).isEqualTo(2);
    }
}
