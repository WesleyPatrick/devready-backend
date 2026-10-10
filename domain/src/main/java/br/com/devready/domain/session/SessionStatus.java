package br.com.devready.domain.session;

public enum SessionStatus {
    IN_PROGRESS,
    SUBMITTED,
    COMPLETED,
    FAILED;

    public boolean closed() {
        return this == COMPLETED || this == FAILED;
    }
}
