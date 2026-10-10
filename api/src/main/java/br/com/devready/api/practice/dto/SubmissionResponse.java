package br.com.devready.api.practice.dto;

import br.com.devready.application.practice.dto.SubmissionView;
import br.com.devready.domain.evaluation.CodeSubmission;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SubmissionResponse(
        UUID id,
        String challengeSlug,
        String kind,
        String language,
        String sourceCode,
        String status,
        int passedCount,
        int totalCount,
        String compileOutput,
        String errorMessage,
        int xpGained,
        Instant createdAt,
        Instant finishedAt,
        List<CaseResultResponse> cases,
        boolean sealed) {

    public static SubmissionResponse from(SubmissionView view) {
        CodeSubmission s = view.submission();
        boolean sealed = view.sealed();
        return new SubmissionResponse(
                s.id(),
                view.challengeSlug(),
                s.kind().name(),
                s.language().name(),
                s.sourceCode(),
                s.status().name(),
                s.passedCount(),
                s.totalCount(),
                sealed ? null : s.compileOutput(),
                sealed ? null : s.errorMessage(),
                s.xpGained(),
                s.createdAt(),
                s.finishedAt(),
                view.cases().stream().map(CaseResultResponse::from).toList(),
                sealed);
    }
}
