package br.com.devready.domain.evaluation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.TestCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerdictTest {

    private static final String MARKER = "#devready-verdict#";

    private static final List<TestCase> CASES =
            List.of(
                    new TestCase(UUID.randomUUID(), 0, "[1]", "2", true),
                    new TestCase(UUID.randomUUID(), 1, "[2]", "4", true),
                    new TestCase(UUID.randomUUID(), 2, "[3]", "6", false),
                    new TestCase(UUID.randomUUID(), 3, "[4]", "8", false));

    private static String line(int position, String output) {
        return MARKER
                + "{\"position\":"
                + position
                + ",\"output\":"
                + output
                + ",\"error\":null,\"timeMs\":2}\n";
    }

    private static ExecutionReport finished(String stdout) {
        return new ExecutionReport(ExecutionOutcome.FINISHED, stdout, "", null, null);
    }

    @Test
    void everyCasePassingIsAccepted() {
        Verdict verdict =
                Verdict.of(
                        finished(line(0, "2") + line(1, "4.0") + line(2, "6") + line(3, "8")),
                        MARKER,
                        CASES,
                        ComparisonMode.EXACT);

        assertThat(verdict.status()).isEqualTo(SubmissionStatus.PASSED);
        assertThat(verdict.results())
                .extracting(CaseResult::status)
                .containsOnly(CaseStatus.PASSED);
        assertThat(verdict.results().get(1).actualJson()).isEqualTo("4");
        assertThat(verdict.results().get(2).visible()).isFalse();
        assertThat(verdict.results().get(0).timeMs()).isEqualTo(2);
    }

    @Test
    void aWrongAnswerFailsKeepingWhatCameOut() {
        String stdout =
                line(3, "8")
                        + line(0, "2")
                        + MARKER
                        + "{\"position\":1,\"output\":null,\"error\":\"IndexError: list index out of range\",\"timeMs\":0}\n"
                        + line(2, "\"seis\"");

        Verdict verdict = Verdict.of(finished(stdout), MARKER, CASES, ComparisonMode.EXACT);

        assertThat(verdict.status()).isEqualTo(SubmissionStatus.FAILED);
        assertThat(verdict.results()).extracting(CaseResult::position).containsExactly(0, 1, 2, 3);
        assertThat(verdict.results())
                .extracting(CaseResult::status)
                .containsExactly(
                        CaseStatus.PASSED, CaseStatus.ERROR, CaseStatus.FAILED, CaseStatus.PASSED);
        assertThat(verdict.results().get(1).errorMessage())
                .isEqualTo("IndexError: list index out of range");
        assertThat(verdict.results().get(1).actualJson()).isNull();
        assertThat(verdict.results().get(2).actualJson()).isEqualTo("\"seis\"");
    }

    @Test
    void aCompileErrorBringsTheCompilerOutputAndNoCase() {
        Verdict verdict =
                Verdict.of(
                        new ExecutionReport(
                                ExecutionOutcome.COMPILE_ERROR,
                                "",
                                "",
                                "  Main.java:3: error: ';' expected\n",
                                null),
                        MARKER,
                        CASES,
                        ComparisonMode.EXACT);

        assertThat(verdict.status()).isEqualTo(SubmissionStatus.COMPILE_ERROR);
        assertThat(verdict.compileOutput()).isEqualTo("Main.java:3: error: ';' expected");
        assertThat(verdict.results()).isEmpty();
    }

    @Test
    void aProgramThatDiesBeforeAnyAnswerIsARuntimeErrorWithTheMessage() {
        Verdict withStderr =
                Verdict.of(
                        new ExecutionReport(
                                ExecutionOutcome.CRASHED,
                                "print sem marcador\n",
                                "SyntaxError: invalid syntax\n",
                                null,
                                "Exited with error status 1"),
                        MARKER,
                        CASES,
                        ComparisonMode.EXACT);
        Verdict withoutStderr =
                Verdict.of(
                        new ExecutionReport(
                                ExecutionOutcome.FINISHED,
                                "",
                                "",
                                null,
                                "Exited with error status 1"),
                        MARKER,
                        CASES,
                        ComparisonMode.EXACT);
        Verdict silent =
                Verdict.of(
                        new ExecutionReport(ExecutionOutcome.CRASHED, "", " ", null, null),
                        MARKER,
                        CASES,
                        ComparisonMode.EXACT);

        assertThat(withStderr.status()).isEqualTo(SubmissionStatus.RUNTIME_ERROR);
        assertThat(withStderr.errorMessage()).isEqualTo("SyntaxError: invalid syntax");
        assertThat(withStderr.results()).isEmpty();
        assertThat(withoutStderr.status()).isEqualTo(SubmissionStatus.RUNTIME_ERROR);
        assertThat(withoutStderr.errorMessage()).isEqualTo("Exited with error status 1");
        assertThat(silent.errorMessage()).isEqualTo("O programa parou sem responder nenhum caso.");
    }

    @Test
    void aTimeLimitKeepsTheCasesThatAnsweredAndMarksTheRest() {
        Verdict partial =
                Verdict.of(
                        new ExecutionReport(
                                ExecutionOutcome.TIME_LIMIT,
                                line(0, "2") + line(1, "4"),
                                "",
                                null,
                                null),
                        MARKER,
                        CASES,
                        ComparisonMode.EXACT);
        Verdict none =
                Verdict.of(
                        new ExecutionReport(ExecutionOutcome.TIME_LIMIT, "", "", null, null),
                        MARKER,
                        CASES,
                        ComparisonMode.EXACT);

        assertThat(partial.status()).isEqualTo(SubmissionStatus.TIME_LIMIT);
        assertThat(partial.results())
                .extracting(CaseResult::status)
                .containsExactly(
                        CaseStatus.PASSED,
                        CaseStatus.PASSED,
                        CaseStatus.TIME_LIMIT,
                        CaseStatus.TIME_LIMIT);
        assertThat(none.status()).isEqualTo(SubmissionStatus.TIME_LIMIT);
        assertThat(none.results())
                .extracting(CaseResult::status)
                .containsOnly(CaseStatus.TIME_LIMIT);
    }

    @Test
    void aCrashAfterSomeAnswersFailsAndTheMissingCasesAreErrors() {
        Verdict verdict =
                Verdict.of(
                        new ExecutionReport(
                                ExecutionOutcome.CRASHED, line(0, "2"), "Killed", null, null),
                        MARKER,
                        CASES,
                        ComparisonMode.EXACT);

        assertThat(verdict.status()).isEqualTo(SubmissionStatus.FAILED);
        assertThat(verdict.results().get(1).status()).isEqualTo(CaseStatus.ERROR);
        assertThat(verdict.results().get(1).errorMessage())
                .isEqualTo("O programa parou antes de responder este caso.");
    }

    @Test
    void aLineWithAnotherMarkerDoesNotCount() {
        Verdict verdict =
                Verdict.of(
                        finished(line(0, "2").replace(MARKER, "#outro-marcador#")),
                        MARKER,
                        CASES,
                        ComparisonMode.EXACT);

        assertThat(verdict.status()).isEqualTo(SubmissionStatus.RUNTIME_ERROR);
    }

    @Test
    void anOutputTooBigToShowIsComparedButNotKept() {
        String big = "\"" + "x".repeat(9_000) + "\"";

        Verdict verdict =
                Verdict.of(
                        finished(line(0, big)), MARKER, CASES.subList(0, 1), ComparisonMode.EXACT);

        assertThat(verdict.results().get(0).status()).isEqualTo(CaseStatus.FAILED);
        assertThat(verdict.results().get(0).actualJson()).isNull();
    }

    @Test
    void longMessagesAreClipped() {
        Verdict verdict =
                Verdict.of(
                        new ExecutionReport(
                                ExecutionOutcome.COMPILE_ERROR, "", "", "e".repeat(10_000), null),
                        MARKER,
                        CASES,
                        ComparisonMode.EXACT);

        assertThat(verdict.compileOutput()).hasSize(Clip.MAX_CHARS + 2).endsWith("\n…");
    }

    @Test
    void aVerdictNeedsAFinalStatusOfTheProgram() {
        assertThatThrownBy(() -> new Verdict(SubmissionStatus.RUNNING, List.of(), null, null))
                .isInstanceOf(EvaluationException.class);
        assertThatThrownBy(() -> new Verdict(SubmissionStatus.ERROR, List.of(), null, null))
                .isInstanceOf(EvaluationException.class);
    }
}
