package br.com.devready.infrastructure.evaluation.persistence;

import br.com.devready.domain.evaluation.CaseResult;
import br.com.devready.domain.evaluation.CaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class SubmissionResultEmbeddable {

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private boolean visible;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status;

    @Column(name = "actual_json", columnDefinition = "text")
    private String actualJson;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "time_ms")
    private Integer timeMs;

    protected SubmissionResultEmbeddable() {}

    static SubmissionResultEmbeddable from(CaseResult result) {
        SubmissionResultEmbeddable row = new SubmissionResultEmbeddable();
        row.position = result.position();
        row.visible = result.visible();
        row.status = result.status();
        row.actualJson = result.actualJson();
        row.errorMessage = result.errorMessage();
        row.timeMs = result.timeMs();
        return row;
    }

    CaseResult toDomain() {
        return new CaseResult(position, visible, status, actualJson, errorMessage, timeMs);
    }

    int position() {
        return position;
    }
}
