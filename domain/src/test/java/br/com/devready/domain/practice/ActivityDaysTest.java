package br.com.devready.domain.practice;

import static br.com.devready.domain.practice.AnswerFixtures.ANA;
import static br.com.devready.domain.practice.AnswerFixtures.DAY;
import static br.com.devready.domain.practice.AnswerFixtures.answer;
import static br.com.devready.domain.practice.AnswerFixtures.at;
import static br.com.devready.domain.practice.AnswerFixtures.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.evaluation.SubmissionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivityDaysTest {

    private final UUID question = UUID.randomUUID();
    private final UUID challenge = UUID.randomUUID();

    @Test
    void countsAnswersCountedSubmissionsAndCompletedSimulationsByTheDayInBrasilia() {
        Map<LocalDate, Integer> days =
                ActivityDays.of(
                        List.of(
                                answer(ANA, question, true, 20, at(0, 9, 0)),
                                answer(ANA, question, false, 0, at(-1, 23, 59))),
                        List.of(
                                attempt(ANA, challenge, SubmissionStatus.FAILED, 0, at(0, 10, 0)),
                                attempt(ANA, challenge, SubmissionStatus.ERROR, 0, at(0, 11, 0))),
                        List.of(new CompletedSimulation(at(1, 0, 0), 63)));

        assertThat(days)
                .containsEntry(DAY, 2)
                .containsEntry(DAY.minusDays(1), 1)
                .containsEntry(DAY.plusDays(1), 1)
                .hasSize(3);
    }

    @Test
    void anEmptySimulationIsNotAnActivity() {
        CompletedSimulation empty = new CompletedSimulation(at(0, 9, 0), 0);

        assertThat(empty.counts()).isFalse();
        assertThat(new CompletedSimulation(at(0, 9, 0), 10).counts()).isTrue();
        assertThat(ActivityDays.of(List.of(), List.of(), List.of(empty, empty, empty))).isEmpty();
    }

    @Test
    void goalDaysAreTheDaysWithThreeActivitiesOrMoreInTheWholeHistory() {
        Map<LocalDate, Integer> days =
                Map.of(DAY.minusDays(400), 3, DAY.minusDays(2), 5, DAY.minusDays(1), 2, DAY, 3);

        assertThat(ActivityDays.goalDays(days)).isEqualTo(3);
    }

    @Test
    void aCompletedSimulationNeedsTheTimeAndAPositiveBonus() {
        assertThatThrownBy(() -> new CompletedSimulation(null, 10))
                .isInstanceOf(PracticeException.class);
        assertThatThrownBy(() -> new CompletedSimulation(at(0, 9, 0), -1))
                .isInstanceOf(PracticeException.class);
    }
}
