package br.com.devready.domain.career;

import static br.com.devready.domain.career.QuestionProgress.ATTEMPTED;
import static br.com.devready.domain.career.QuestionProgress.NOT_STARTED;
import static br.com.devready.domain.career.QuestionProgress.SOLVED;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class CompetencyStatusTest {

    @Test
    void aCompetencyWithoutQuestionsIsNotStarted() {
        assertThat(CompetencyStatus.of(List.of())).isEqualTo(CompetencyStatus.NOT_STARTED);
    }

    @Test
    void nothingAnsweredIsNotStarted() {
        assertThat(CompetencyStatus.of(List.of(NOT_STARTED, NOT_STARTED)))
                .isEqualTo(CompetencyStatus.NOT_STARTED);
    }

    @Test
    void anyAnswerWithoutAllSolvedIsInProgress() {
        assertThat(CompetencyStatus.of(List.of(ATTEMPTED, NOT_STARTED)))
                .isEqualTo(CompetencyStatus.IN_PROGRESS);
        assertThat(CompetencyStatus.of(List.of(SOLVED, NOT_STARTED)))
                .isEqualTo(CompetencyStatus.IN_PROGRESS);
        assertThat(CompetencyStatus.of(List.of(SOLVED, ATTEMPTED)))
                .isEqualTo(CompetencyStatus.IN_PROGRESS);
    }

    @Test
    void allSolvedIsDone() {
        assertThat(CompetencyStatus.of(List.of(SOLVED))).isEqualTo(CompetencyStatus.DONE);
        assertThat(CompetencyStatus.of(List.of(SOLVED, SOLVED))).isEqualTo(CompetencyStatus.DONE);
    }
}
