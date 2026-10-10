package br.com.devready.domain.practice;

import static br.com.devready.domain.practice.AnswerFixtures.ANA;
import static br.com.devready.domain.practice.AnswerFixtures.answer;
import static br.com.devready.domain.practice.AnswerFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnswerTest {

    @Test
    void keepsWhatWasAnswered() {
        UUID question = UUID.randomUUID();

        Answer answer = answer(ANA, question, true, 30, at(0, 10, 0));

        assertThat(answer.userId()).isEqualTo(ANA);
        assertThat(answer.questionId()).isEqualTo(question);
        assertThat(answer.correct()).isTrue();
        assertThat(answer.xpGained()).isEqualTo(30);
        assertThat(answer.answeredAt()).isEqualTo(at(0, 10, 0));
    }

    @Test
    void refusesAnIncompleteAnswer() {
        assertThatThrownBy(
                        () ->
                                new Answer(
                                        null,
                                        ANA,
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        false,
                                        0,
                                        at(0, 10, 0)))
                .isInstanceOf(PracticeException.class);
        assertThatThrownBy(
                        () ->
                                new Answer(
                                        UUID.randomUUID(),
                                        ANA,
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        false,
                                        0,
                                        null))
                .isInstanceOf(PracticeException.class);
    }

    @Test
    void xpIsNeverNegativeAndAWrongAnswerEarnsNone() {
        assertThatThrownBy(() -> answer(ANA, UUID.randomUUID(), true, -1, at(0, 10, 0)))
                .isInstanceOf(PracticeException.class)
                .hasMessage("Resposta com XP negativo.");
        assertThatThrownBy(() -> answer(ANA, UUID.randomUUID(), false, 20, at(0, 10, 0)))
                .isInstanceOf(PracticeException.class)
                .hasMessage("Resposta errada não ganha XP.");
    }
}
