package br.com.devready.domain.evaluation;

import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.TestCase;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record Verdict(
        SubmissionStatus status,
        List<CaseResult> results,
        String compileOutput,
        String errorMessage) {

    static final String NO_ANSWER = "O programa parou antes de responder este caso.";
    static final String SILENT_CRASH = "O programa parou sem responder nenhum caso.";

    public Verdict {
        if (status == null || !status.finished() || status == SubmissionStatus.ERROR) {
            throw new EvaluationException(
                    "Veredito precisa de uma situação final do programa: " + status + ".");
        }
        results = results == null ? List.of() : List.copyOf(results);
    }

    public static Verdict of(
            ExecutionReport report, String marker, List<TestCase> cases, ComparisonMode mode) {
        if (report.outcome() == ExecutionOutcome.COMPILE_ERROR) {
            return new Verdict(
                    SubmissionStatus.COMPILE_ERROR,
                    List.of(),
                    Clip.of(report.compileOutput()),
                    null);
        }
        List<TestCase> ordered =
                cases.stream().sorted(Comparator.comparingInt(TestCase::position)).toList();
        Map<Integer, HarnessLine> lines = HarnessOutput.parse(report.stdout(), marker);
        boolean answeredAny = ordered.stream().anyMatch(c -> lines.containsKey(c.position()));
        boolean timedOut = report.outcome() == ExecutionOutcome.TIME_LIMIT;
        if (!answeredAny && !timedOut) {
            String message = Clip.of(report.stderr());
            if (message == null) {
                message = Clip.of(report.message());
            }
            return new Verdict(
                    SubmissionStatus.RUNTIME_ERROR,
                    List.of(),
                    null,
                    message == null ? SILENT_CRASH : message);
        }
        List<CaseResult> results = new ArrayList<>();
        for (TestCase testCase : ordered) {
            results.add(judge(testCase, lines.get(testCase.position()), timedOut, mode));
        }
        return new Verdict(statusOf(results), results, null, null);
    }

    private static CaseResult judge(
            TestCase testCase, HarnessLine line, boolean timedOut, ComparisonMode mode) {
        int position = testCase.position();
        boolean visible = testCase.visible();
        if (line == null) {
            return timedOut
                    ? new CaseResult(position, visible, CaseStatus.TIME_LIMIT, null, null, null)
                    : new CaseResult(position, visible, CaseStatus.ERROR, null, NO_ANSWER, null);
        }
        if (line.error() != null) {
            return new CaseResult(
                    position,
                    visible,
                    CaseStatus.ERROR,
                    null,
                    Clip.of(line.error()),
                    line.timeMs());
        }
        boolean passed =
                OutputComparison.matches(Json.parse(testCase.expectedJson()), line.output(), mode);
        String actual = Json.write(line.output());
        return new CaseResult(
                position,
                visible,
                passed ? CaseStatus.PASSED : CaseStatus.FAILED,
                actual.length() <= Clip.MAX_CHARS ? actual : null,
                null,
                line.timeMs());
    }

    private static SubmissionStatus statusOf(List<CaseResult> results) {
        if (results.stream().allMatch(r -> r.status() == CaseStatus.PASSED)) {
            return SubmissionStatus.PASSED;
        }
        if (results.stream().anyMatch(r -> r.status() == CaseStatus.TIME_LIMIT)) {
            return SubmissionStatus.TIME_LIMIT;
        }
        return SubmissionStatus.FAILED;
    }
}
