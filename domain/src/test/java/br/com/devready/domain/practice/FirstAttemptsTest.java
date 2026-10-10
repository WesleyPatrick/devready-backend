package br.com.devready.domain.practice;

import static br.com.devready.domain.practice.AnswerFixtures.ANA;
import static br.com.devready.domain.practice.AnswerFixtures.BIA;
import static br.com.devready.domain.practice.AnswerFixtures.answer;
import static br.com.devready.domain.practice.AnswerFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FirstAttemptsTest {

    private final UUID question = UUID.randomUUID();

    @Test
    void belowFivePeopleThereIsNoRate() {
        assertThat(new FirstAttempts(4, 4).acceptance()).isEmpty();
        assertThat(new FirstAttempts(0, 0).acceptance()).isEmpty();
    }

    @Test
    void withFivePeopleTheRateIsWhoGotItRightTheFirstTime() {
        assertThat(new FirstAttempts(5, 3).acceptance()).isEqualTo(OptionalDouble.of(0.6));
    }

    @Test
    void onlyTheFirstAnswerOfEachPersonCountsWhateverTheOrderOfTheList() {
        UUID carla = UUID.randomUUID();
        List<Answer> answers = new ArrayList<>();
        answers.add(answer(ANA, question, true, 30, at(0, 11, 0)));
        answers.add(answer(ANA, question, false, 0, at(0, 10, 0)));
        answers.add(answer(BIA, question, true, 30, at(0, 12, 0)));
        answers.add(answer(BIA, question, false, 0, at(0, 9, 0)));
        answers.add(answer(carla, question, false, 0, at(0, 13, 0)));
        answers.add(answer(carla, question, true, 30, at(0, 8, 0)));
        for (int i = 0; i < 2; i++) {
            answers.add(answer(UUID.randomUUID(), question, true, 30, at(0, 14, i)));
        }

        FirstAttempts attempts = FirstAttempts.byQuestion(answers).get(question);

        assertThat(attempts).isEqualTo(new FirstAttempts(5, 3));
        assertThat(attempts.acceptance()).isEqualTo(OptionalDouble.of(0.6));
    }

    @Test
    void separatesTheQuestions() {
        UUID other = UUID.randomUUID();

        Map<UUID, FirstAttempts> attempts =
                FirstAttempts.byQuestion(
                        List.of(
                                answer(ANA, question, true, 20, at(0, 9, 0)),
                                answer(ANA, other, false, 0, at(0, 9, 1)),
                                answer(BIA, other, true, 20, at(0, 9, 2))));

        assertThat(attempts)
                .containsOnly(
                        Map.entry(question, new FirstAttempts(1, 1)),
                        Map.entry(other, new FirstAttempts(2, 1)));
    }

    @Test
    void refusesImpossibleCounts() {
        assertThatThrownBy(() -> new FirstAttempts(2, 3)).isInstanceOf(PracticeException.class);
        assertThatThrownBy(() -> new FirstAttempts(-1, 0)).isInstanceOf(PracticeException.class);
    }
}
