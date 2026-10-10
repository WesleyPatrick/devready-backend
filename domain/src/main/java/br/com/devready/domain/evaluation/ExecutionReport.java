package br.com.devready.domain.evaluation;

public record ExecutionReport(
        ExecutionOutcome outcome,
        String stdout,
        String stderr,
        String compileOutput,
        String message) {
    public ExecutionReport {
        if (outcome == null) {
            throw new EvaluationException("Execução sem desfecho.");
        }
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
    }
}
