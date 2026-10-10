package br.com.devready.domain.session;

import static br.com.devready.domain.session.SessionFixtures.T0;
import static br.com.devready.domain.session.SessionFixtures.id;
import static br.com.devready.domain.session.SessionFixtures.submit;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.evaluation.SubmissionStatus;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ScoreTest {

    private static List<SessionQuestion> questions(boolean... hits) {
        List<SessionQuestion> list = new java.util.ArrayList<>();
        for (int i = 0; i < hits.length; i++) {
            list.add(SessionQuestion.of(i + 1, id(i + 1)).answer(id(50 + i), hits[i]));
        }
        return list;
    }

    @Test
    void theFinalScoreWeighsObjectivesAndChallenges() {
        Score score =
                Score.simulation(
                        questions(true, true, true, false),
                        List.of(
                                Optional.of(submit(id(11), SubmissionStatus.FAILED, 2, T0)),
                                Optional.empty()),
                        0.7);

        assertThat(score.objectiveScore()).isEqualTo(0.75);
        assertThat(score.challengeScore()).isEqualTo(0.25);
        assertThat(score.finalScore()).isEqualTo(0.6);
    }

    @Test
    void anUnansweredQuestionCountsAsWrong() {
        List<SessionQuestion> list =
                List.of(
                        SessionQuestion.of(1, id(1)).answer(id(50), true),
                        SessionQuestion.of(2, id(2)));

        assertThat(Score.lesson(list).objectiveScore()).isEqualTo(0.5);
    }

    @Test
    void theLessonHasOnlyTheObjectiveScoreWithFourDecimals() {
        Score score = Score.lesson(questions(true, true, false));

        assertThat(score.objectiveScore()).isEqualTo(0.6667);
        assertThat(score.challengeScore()).isNull();
        assertThat(score.finalScore()).isNull();
    }

    @Test
    void aSimulationWithoutChallengesIsTheObjectiveScore() {
        Score score = Score.simulation(questions(true, false), List.of(), 0.5);

        assertThat(score.challengeScore()).isNull();
        assertThat(score.finalScore()).isEqualTo(0.5);
    }
}
