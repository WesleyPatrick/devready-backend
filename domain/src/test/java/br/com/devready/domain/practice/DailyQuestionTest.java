package br.com.devready.domain.practice;

import static br.com.devready.domain.practice.AnswerFixtures.ANA;
import static br.com.devready.domain.practice.AnswerFixtures.answer;
import static br.com.devready.domain.practice.AnswerFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DailyQuestionTest {

    private final UUID first = UUID.randomUUID();
    private final UUID second = UUID.randomUUID();
    private final UUID third = UUID.randomUUID();
    private final List<UUID> track = List.of(first, second, third);

    @Test
    void aTrackWithoutQuestionsHasNoDailyQuestion() {
        assertThat(DailyQuestion.pick(List.of(), List.of(), Set.of(), at(0, 9, 0))).isEmpty();
    }

    @Test
    void startsWithTheFirstQuestionOfTheTrack() {
        assertThat(DailyQuestion.pick(track, List.of(), Set.of(), at(0, 9, 0))).contains(first);
    }

    @Test
    void skipsWhatWasSolvedBeforeTheDayStarted() {
        List<Answer> answers = List.of(answer(ANA, first, true, 20, at(-1, 20, 0)));

        assertThat(DailyQuestion.pick(track, answers, Set.of(), at(0, 9, 0))).contains(second);
    }

    @Test
    void whatWasSolvedInASimulationIsSolvedToo() {
        assertThat(DailyQuestion.pick(track, List.of(), Set.of(first), at(0, 9, 0)))
                .contains(second);
        assertThat(DailyQuestion.pick(track, List.of(), Set.of(first, second, third), at(0, 9, 0)))
                .contains(first);
    }

    @Test
    void staysTheSameDuringTheDayEvenAfterBeingSolvedAndChangesAtMidnight() {
        List<Answer> answers = List.of(answer(ANA, first, true, 20, at(0, 9, 0)));

        assertThat(DailyQuestion.pick(track, answers, Set.of(), at(0, 23, 59))).contains(first);
        assertThat(DailyQuestion.pick(track, answers, Set.of(), at(1, 0, 0))).contains(second);
    }

    @Test
    void turnsAtMidnightInBrasilia() {
        List<Answer> answers = List.of(answer(ANA, first, true, 20, at(-1, 23, 59)));

        assertThat(DailyQuestion.pick(track, answers, Set.of(), at(-1, 23, 59).plusSeconds(30)))
                .contains(first);
        assertThat(DailyQuestion.pick(track, answers, Set.of(), at(0, 0, 0))).contains(second);
    }

    @Test
    void anAnswerAtExactMidnightBelongsToTodayAndKeepsTheDailyQuestion() {
        List<Answer> answers = List.of(answer(ANA, first, true, 20, at(0, 0, 0)));

        assertThat(DailyQuestion.pick(track, answers, Set.of(), at(0, 9, 0))).contains(first);
    }

    @Test
    void aWrongAnswerDoesNotCountAsSolved() {
        List<Answer> answers = List.of(answer(ANA, first, false, 0, at(-1, 20, 0)));

        assertThat(DailyQuestion.pick(track, answers, Set.of(), at(0, 9, 0))).contains(first);
    }

    @Test
    void withEverythingSolvedItGoesBackToTheFirst() {
        List<Answer> answers =
                List.of(
                        answer(ANA, first, true, 20, at(-2, 9, 0)),
                        answer(ANA, second, true, 20, at(-2, 9, 1)),
                        answer(ANA, third, true, 20, at(-2, 9, 2)));

        assertThat(DailyQuestion.pick(track, answers, Set.of(), at(0, 9, 0))).contains(first);
    }

    @Test
    void answersToQuestionsOutsideTheTrackDoNotMatter() {
        List<Answer> answers = List.of(answer(ANA, UUID.randomUUID(), true, 20, at(-1, 9, 0)));

        assertThat(DailyQuestion.pick(track, answers, Set.of(), at(0, 9, 0))).contains(first);
    }
}
