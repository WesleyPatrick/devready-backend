package br.com.devready.domain.practice;

import static br.com.devready.domain.practice.AnswerFixtures.ANA;
import static br.com.devready.domain.practice.AnswerFixtures.answer;
import static br.com.devready.domain.practice.AnswerFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionStatusTest {

    private final UUID question = UUID.randomUUID();

    @Test
    void withoutAnswersItIsNotStarted() {
        assertThat(QuestionStatus.of(List.of())).isEqualTo(QuestionStatus.NOT_STARTED);
    }

    @Test
    void onlyWrongAnswersMeanAttempted() {
        assertThat(
                        QuestionStatus.of(
                                List.of(
                                        answer(ANA, question, false, 0, at(0, 9, 0)),
                                        answer(ANA, question, false, 0, at(0, 10, 0)))))
                .isEqualTo(QuestionStatus.ATTEMPTED);
    }

    @Test
    void anyRightAnswerMeansSolvedAndItNeverGoesBack() {
        assertThat(
                        QuestionStatus.of(
                                List.of(
                                        answer(ANA, question, false, 0, at(0, 9, 0)),
                                        answer(ANA, question, true, 30, at(0, 10, 0)))))
                .isEqualTo(QuestionStatus.SOLVED);
        assertThat(
                        QuestionStatus.of(
                                List.of(
                                        answer(ANA, question, true, 30, at(0, 9, 0)),
                                        answer(ANA, question, false, 0, at(0, 10, 0)))))
                .isEqualTo(QuestionStatus.SOLVED);
    }

    @Test
    void byQuestionGroupsTheAnswersOfEachQuestion() {
        UUID other = UUID.randomUUID();

        Map<UUID, QuestionStatus> statuses =
                QuestionStatus.byQuestion(
                        List.of(
                                answer(ANA, question, false, 0, at(0, 9, 0)),
                                answer(ANA, other, false, 0, at(0, 9, 5)),
                                answer(ANA, other, true, 20, at(0, 9, 10))));

        assertThat(statuses)
                .containsOnly(
                        Map.entry(question, QuestionStatus.ATTEMPTED),
                        Map.entry(other, QuestionStatus.SOLVED));
    }

    @Test
    void solvedElsewhereOverridesWhatTheAnswersSay() {
        UUID other = UUID.randomUUID();
        UUID fresh = UUID.randomUUID();
        Map<UUID, QuestionStatus> statuses =
                QuestionStatus.withSolved(
                        Map.of(question, QuestionStatus.ATTEMPTED, other, QuestionStatus.ATTEMPTED),
                        List.of(question, fresh));

        assertThat(statuses)
                .containsEntry(question, QuestionStatus.SOLVED)
                .containsEntry(fresh, QuestionStatus.SOLVED)
                .containsEntry(other, QuestionStatus.ATTEMPTED);
    }
}
