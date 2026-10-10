package br.com.devready.domain.practice;

import static br.com.devready.domain.practice.AnswerFixtures.ANA;
import static br.com.devready.domain.practice.AnswerFixtures.BIA;
import static br.com.devready.domain.practice.AnswerFixtures.DAY;
import static br.com.devready.domain.practice.AnswerFixtures.answer;
import static br.com.devready.domain.practice.AnswerFixtures.at;
import static br.com.devready.domain.practice.AnswerFixtures.attempt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.evaluation.SubmissionStatus;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChallengeAttemptTest {

    private final UUID challenge = UUID.randomUUID();

    @Test
    void onlyAFinishedSubmissionThatIsNotASystemErrorCounts() {
        for (SubmissionStatus status : SubmissionStatus.values()) {
            boolean counts =
                    status != SubmissionStatus.QUEUED
                            && status != SubmissionStatus.RUNNING
                            && status != SubmissionStatus.ERROR;
            ChallengeAttempt attempt = attempt(ANA, challenge, status, 0, at(0, 10, 0));
            assertThat(attempt.counts()).as(status.name()).isEqualTo(counts);
            assertThat(attempt.passed())
                    .as(status.name())
                    .isEqualTo(status == SubmissionStatus.PASSED);
        }
    }

    @Test
    void refusesAnIncompleteAttemptOrXpOutsideAnAcceptedOne() {
        assertThatThrownBy(() -> attempt(null, challenge, SubmissionStatus.FAILED, 0, at(0, 9, 0)))
                .isInstanceOf(PracticeException.class);
        assertThatThrownBy(() -> attempt(ANA, challenge, SubmissionStatus.FAILED, 0, null))
                .isInstanceOf(PracticeException.class);
        assertThatThrownBy(() -> attempt(ANA, challenge, SubmissionStatus.PASSED, -1, at(0, 9, 0)))
                .isInstanceOf(PracticeException.class);
        assertThatThrownBy(() -> attempt(ANA, challenge, SubmissionStatus.FAILED, 40, at(0, 9, 0)))
                .isInstanceOf(PracticeException.class)
                .hasMessage("Só envio aceito ganha XP.");
    }

    @Test
    void anAcceptedSubmissionSolvesTheChallengeAndItNeverGoesBack() {
        assertThat(
                        QuestionStatus.ofAttempts(
                                List.of(
                                        attempt(
                                                ANA,
                                                challenge,
                                                SubmissionStatus.FAILED,
                                                0,
                                                at(0, 9, 0)),
                                        attempt(
                                                ANA,
                                                challenge,
                                                SubmissionStatus.PASSED,
                                                40,
                                                at(0, 10, 0)))))
                .isEqualTo(QuestionStatus.SOLVED);
        assertThat(
                        QuestionStatus.ofAttempts(
                                List.of(
                                        attempt(
                                                ANA,
                                                challenge,
                                                SubmissionStatus.PASSED,
                                                40,
                                                at(0, 9, 0)),
                                        attempt(
                                                ANA,
                                                challenge,
                                                SubmissionStatus.FAILED,
                                                0,
                                                at(0, 10, 0)))))
                .isEqualTo(QuestionStatus.SOLVED);
    }

    @Test
    void finishedWithoutAcceptanceIsAttemptedButSystemErrorsAndUnfinishedDoNotCount() {
        for (SubmissionStatus failed :
                List.of(
                        SubmissionStatus.FAILED,
                        SubmissionStatus.COMPILE_ERROR,
                        SubmissionStatus.RUNTIME_ERROR,
                        SubmissionStatus.TIME_LIMIT)) {
            assertThat(
                            QuestionStatus.ofAttempts(
                                    List.of(attempt(ANA, challenge, failed, 0, at(0, 9, 0)))))
                    .as(failed.name())
                    .isEqualTo(QuestionStatus.ATTEMPTED);
        }
        assertThat(
                        QuestionStatus.ofAttempts(
                                List.of(
                                        attempt(
                                                ANA,
                                                challenge,
                                                SubmissionStatus.ERROR,
                                                0,
                                                at(0, 9, 0)),
                                        attempt(
                                                ANA,
                                                challenge,
                                                SubmissionStatus.QUEUED,
                                                0,
                                                at(0, 9, 1)),
                                        attempt(
                                                ANA,
                                                challenge,
                                                SubmissionStatus.RUNNING,
                                                0,
                                                at(0, 9, 2)))))
                .isEqualTo(QuestionStatus.NOT_STARTED);
        assertThat(QuestionStatus.ofAttempts(List.of())).isEqualTo(QuestionStatus.NOT_STARTED);
    }

    @Test
    void byChallengeGroupsTheSubmissionsAndLeavesOutWhatDoesNotCount() {
        UUID other = UUID.randomUUID();
        UUID onlyErrors = UUID.randomUUID();

        Map<UUID, QuestionStatus> statuses =
                QuestionStatus.byChallenge(
                        List.of(
                                attempt(ANA, challenge, SubmissionStatus.FAILED, 0, at(0, 9, 0)),
                                attempt(ANA, other, SubmissionStatus.PASSED, 60, at(0, 9, 5)),
                                attempt(ANA, onlyErrors, SubmissionStatus.ERROR, 0, at(0, 9, 6))));

        assertThat(statuses)
                .containsOnly(
                        Map.entry(challenge, QuestionStatus.ATTEMPTED),
                        Map.entry(other, QuestionStatus.SOLVED));
    }

    @Test
    void theRateCountsTheFirstSubmissionThatCountsOfEachPerson() {
        UUID carla = UUID.randomUUID();
        List<ChallengeAttempt> attempts =
                List.of(
                        attempt(ANA, challenge, SubmissionStatus.ERROR, 0, at(0, 9, 0)),
                        attempt(ANA, challenge, SubmissionStatus.PASSED, 40, at(0, 11, 0)),
                        attempt(ANA, challenge, SubmissionStatus.FAILED, 0, at(0, 10, 0)),
                        attempt(BIA, challenge, SubmissionStatus.PASSED, 40, at(0, 8, 0)),
                        attempt(carla, challenge, SubmissionStatus.PASSED, 40, at(0, 12, 0)),
                        attempt(carla, challenge, SubmissionStatus.COMPILE_ERROR, 0, at(0, 7, 0)),
                        attempt(
                                UUID.randomUUID(),
                                challenge,
                                SubmissionStatus.PASSED,
                                40,
                                at(0, 13, 0)),
                        attempt(
                                UUID.randomUUID(),
                                challenge,
                                SubmissionStatus.PASSED,
                                40,
                                at(0, 14, 0)),
                        attempt(
                                UUID.randomUUID(),
                                challenge,
                                SubmissionStatus.QUEUED,
                                0,
                                at(0, 15, 0)));

        FirstAttempts first = FirstAttempts.byChallenge(attempts).get(challenge);

        assertThat(first).isEqualTo(new FirstAttempts(5, 3));
        assertThat(first.acceptance()).isEqualTo(OptionalDouble.of(0.6));
    }

    @Test
    void aChallengeIsWorthTwiceAQuestion() {
        assertThat(Xp.challengeFirstSolve(Difficulty.EASY)).isEqualTo(40);
        assertThat(Xp.challengeFirstSolve(Difficulty.MEDIUM)).isEqualTo(60);
        assertThat(Xp.challengeFirstSolve(Difficulty.HARD)).isEqualTo(80);
    }

    @Test
    void submissionsCountForXpGoalStreakAndCalendarLikeAnswers() {
        UUID question = UUID.randomUUID();

        PracticeProgress progress =
                PracticeProgress.of(
                        List.of(answer(ANA, question, true, 20, at(0, 9, 0))),
                        List.of(
                                attempt(ANA, challenge, SubmissionStatus.FAILED, 0, at(-1, 23, 59)),
                                attempt(ANA, challenge, SubmissionStatus.PASSED, 40, at(0, 0, 0)),
                                attempt(ANA, challenge, SubmissionStatus.ERROR, 0, at(0, 11, 0)),
                                attempt(ANA, challenge, SubmissionStatus.QUEUED, 0, at(0, 11, 30))),
                        List.of(),
                        at(0, 12, 0));

        assertThat(progress.xp()).isEqualTo(60);
        assertThat(progress.streakDays()).isEqualTo(2);
        assertThat(progress.dailyGoal()).isEqualTo(new DailyGoal(2, 3));
        assertThat(progress.activity().get(34).date()).isEqualTo(DAY);
        assertThat(progress.activity().get(34).count()).isEqualTo(2);
        assertThat(progress.activity().get(33).count()).isEqualTo(1);
    }
}
