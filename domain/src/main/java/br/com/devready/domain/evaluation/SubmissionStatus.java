package br.com.devready.domain.evaluation;

public enum SubmissionStatus {
    QUEUED,
    RUNNING,
    PASSED,
    FAILED,
    COMPILE_ERROR,
    RUNTIME_ERROR,
    TIME_LIMIT,
    ERROR;

    public boolean finished() {
        return this != QUEUED && this != RUNNING;
    }
}
