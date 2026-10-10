package br.com.devready.domain.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.catalog.Language;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class CodeSubmissionTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    private static CodeSubmission queued(SubmissionKind kind, int total, int xpReward) {
        return CodeSubmission.queue(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                Language.JAVA,
                "class Solution {}",
                kind,
                total,
                xpReward,
                T0);
    }

    private static Verdict passed(int cases) {
        List<CaseResult> results =
                IntStream.range(0, cases)
                        .mapToObj(i -> new CaseResult(i, i < 2, CaseStatus.PASSED, "1", null, 1))
                        .toList();
        return new Verdict(SubmissionStatus.PASSED, results, null, null);
    }

    @Test
    void bornInTheQueueWithoutResults() {
        CodeSubmission submission = queued(SubmissionKind.SUBMIT, 6, 40);

        assertThat(submission.status()).isEqualTo(SubmissionStatus.QUEUED);
        assertThat(submission.passedCount()).isZero();
        assertThat(submission.totalCount()).isEqualTo(6);
        assertThat(submission.xpReward()).isEqualTo(40);
        assertThat(submission.xpGained()).isZero();
        assertThat(submission.createdAt()).isEqualTo(T0);
        assertThat(submission.finishedAt()).isNull();
        assertThat(submission.results()).isEmpty();
    }

    @Test
    void runsThenFinishesWithTheVerdictAndTheXpOfTheFirstSolve() {
        CodeSubmission running = queued(SubmissionKind.SUBMIT, 4, 60).start();
        Instant end = T0.plusSeconds(3);

        CodeSubmission first = running.finish(passed(4), true, end);
        CodeSubmission again = running.finish(passed(4), false, end);

        assertThat(running.status()).isEqualTo(SubmissionStatus.RUNNING);
        assertThat(first.status()).isEqualTo(SubmissionStatus.PASSED);
        assertThat(first.passedCount()).isEqualTo(4);
        assertThat(first.xpGained()).isEqualTo(60);
        assertThat(first.finishedAt()).isEqualTo(end);
        assertThat(first.results()).hasSize(4);
        assertThat(again.xpGained()).isZero();
    }

    @Test
    void aFailedSubmissionNeverGainsXpAndCountsWhatPassed() {
        Verdict failed =
                new Verdict(
                        SubmissionStatus.FAILED,
                        List.of(
                                new CaseResult(0, true, CaseStatus.PASSED, "1", null, 1),
                                new CaseResult(1, true, CaseStatus.FAILED, "2", null, 1)),
                        null,
                        null);

        CodeSubmission done = queued(SubmissionKind.SUBMIT, 2, 40).start().finish(failed, true, T0);

        assertThat(done.status()).isEqualTo(SubmissionStatus.FAILED);
        assertThat(done.passedCount()).isEqualTo(1);
        assertThat(done.xpGained()).isZero();
    }

    @Test
    void aRunIsWorthNoXp() {
        CodeSubmission run = queued(SubmissionKind.RUN, 2, 0).start().finish(passed(2), true, T0);

        assertThat(run.xpGained()).isZero();
        assertThatThrownBy(() -> queued(SubmissionKind.RUN, 2, 40))
                .isInstanceOf(EvaluationException.class);
    }

    @Test
    void onlyAQueuedSubmissionStartsAndOnlyARunningOneFinishes() {
        CodeSubmission submission = queued(SubmissionKind.SUBMIT, 4, 40);

        assertThatThrownBy(() -> submission.finish(passed(4), true, T0))
                .isInstanceOf(EvaluationException.class);
        assertThatThrownBy(() -> submission.start().start())
                .isInstanceOf(EvaluationException.class);
        CodeSubmission done = submission.start().finish(passed(4), true, T0);
        assertThatThrownBy(done::start).isInstanceOf(EvaluationException.class);
        assertThatThrownBy(() -> done.fail("x", T0)).isInstanceOf(EvaluationException.class);
    }

    @Test
    void aSystemFailureIsAnErrorWithTheMessage() {
        CodeSubmission failed = queued(SubmissionKind.SUBMIT, 4, 40).start().fail(" fora ", T0);

        assertThat(failed.status()).isEqualTo(SubmissionStatus.ERROR);
        assertThat(failed.errorMessage()).isEqualTo("fora");
        assertThat(failed.finishedAt()).isEqualTo(T0);
        assertThat(failed.xpGained()).isZero();
    }

    @Test
    void stuckForMoreThanTwoMinutesBecomesAnError() {
        CodeSubmission waiting = queued(SubmissionKind.SUBMIT, 4, 40);
        Instant limit = T0.plus(CodeSubmission.STUCK_AFTER);

        assertThat(CodeSubmission.STUCK_AFTER).isEqualTo(Duration.ofMinutes(2));
        assertThat(waiting.stuckAt(limit)).isFalse();
        assertThat(waiting.expireAt(limit)).isSameAs(waiting);
        CodeSubmission expired = waiting.start().expireAt(limit.plusSeconds(1));
        assertThat(expired.status()).isEqualTo(SubmissionStatus.ERROR);
        assertThat(expired.errorMessage())
                .isEqualTo("O envio não terminou a tempo. Tente de novo.");
        assertThat(expired.finishedAt()).isEqualTo(limit.plusSeconds(1));
        assertThat(expired.stuckAt(limit.plusSeconds(600))).isFalse();
    }

    @Test
    void refusesAnIncompleteOrImpossibleSubmission() {
        assertThatThrownBy(
                        () ->
                                CodeSubmission.queue(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        Language.JAVA,
                                        "  ",
                                        SubmissionKind.SUBMIT,
                                        4,
                                        40,
                                        T0))
                .isInstanceOf(EvaluationException.class)
                .hasMessage("Envio sem código.");
        assertThatThrownBy(
                        () ->
                                CodeSubmission.queue(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        Language.JAVA,
                                        "x".repeat(CodeSubmission.MAX_SOURCE_CHARS + 1),
                                        SubmissionKind.SUBMIT,
                                        4,
                                        40,
                                        T0))
                .isInstanceOf(EvaluationException.class)
                .hasMessage("Código acima de 64 KB.");
        assertThatThrownBy(() -> queued(SubmissionKind.SUBMIT, 0, 40))
                .isInstanceOf(EvaluationException.class);
        assertThatThrownBy(() -> queued(SubmissionKind.SUBMIT, 4, -1))
                .isInstanceOf(EvaluationException.class);
        assertThatThrownBy(
                        () ->
                                new CodeSubmission(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        Language.JAVA,
                                        "x",
                                        SubmissionKind.SUBMIT,
                                        SubmissionStatus.FAILED,
                                        0,
                                        4,
                                        null,
                                        null,
                                        40,
                                        40,
                                        T0,
                                        T0,
                                        List.of(),
                                        null))
                .isInstanceOf(EvaluationException.class)
                .hasMessage("Só o envio aceito ganha XP, e o que ele vale.");
        assertThatThrownBy(
                        () ->
                                new CodeSubmission(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        Language.JAVA,
                                        "x",
                                        SubmissionKind.SUBMIT,
                                        SubmissionStatus.PASSED,
                                        4,
                                        4,
                                        null,
                                        null,
                                        40,
                                        0,
                                        T0,
                                        null,
                                        List.of(),
                                        null))
                .isInstanceOf(EvaluationException.class)
                .hasMessage("Envio terminado precisa da hora do fim, e só ele.");
        assertThatThrownBy(() -> new CaseResult(-1, true, CaseStatus.PASSED, "1", null, 1))
                .isInstanceOf(EvaluationException.class);
    }

    @Test
    void unfinishedStatusesAreTheQueueAndRunning() {
        assertThat(SubmissionStatus.QUEUED.finished()).isFalse();
        assertThat(SubmissionStatus.RUNNING.finished()).isFalse();
        assertThat(SubmissionStatus.ERROR.finished()).isTrue();
        assertThat(SubmissionStatus.PASSED.finished()).isTrue();
    }

    @Test
    void aSessionSubmissionKeepsItsSessionAndIsWorthNoXp() {
        UUID session = UUID.randomUUID();
        CodeSubmission queued =
                CodeSubmission.queueInSession(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        Language.JAVA,
                        "class Solution {}",
                        SubmissionKind.SUBMIT,
                        4,
                        session,
                        T0);

        assertThat(queued.sessionId()).isEqualTo(session);
        assertThat(queued.xpReward()).isZero();
        CodeSubmission finished = queued.start().finish(passed(4), true, T0.plusSeconds(5));
        assertThat(finished.sessionId()).isEqualTo(session);
        assertThat(finished.xpGained()).isZero();
        assertThat(queued.fail("fora", T0).sessionId()).isEqualTo(session);
        assertThat(queued.expireAt(T0.plus(Duration.ofMinutes(3))).sessionId()).isEqualTo(session);
    }

    @Test
    void aSessionSubmissionWithXpIsABug() {
        assertThatThrownBy(
                        () ->
                                new CodeSubmission(
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        UUID.randomUUID(),
                                        Language.JAVA,
                                        "class Solution {}",
                                        SubmissionKind.SUBMIT,
                                        SubmissionStatus.QUEUED,
                                        0,
                                        4,
                                        null,
                                        null,
                                        40,
                                        0,
                                        T0,
                                        null,
                                        List.of(),
                                        UUID.randomUUID()))
                .isInstanceOf(EvaluationException.class);
    }

    @Test
    void aPracticeSubmissionHasNoSession() {
        assertThat(queued(SubmissionKind.SUBMIT, 4, 40).sessionId()).isNull();
    }
}
