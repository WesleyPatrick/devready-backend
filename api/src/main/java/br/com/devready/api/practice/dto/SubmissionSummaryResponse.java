package br.com.devready.api.practice.dto;

import br.com.devready.domain.evaluation.CodeSubmission;
import java.time.Instant;
import java.util.UUID;

public record SubmissionSummaryResponse(
        UUID id,
        String language,
        String status,
        int passedCount,
        int totalCount,
        Instant createdAt) {
    public static SubmissionSummaryResponse from(CodeSubmission s) {
        return new SubmissionSummaryResponse(
                s.id(),
                s.language().name(),
                s.status().name(),
                s.passedCount(),
                s.totalCount(),
                s.createdAt());
    }
}
