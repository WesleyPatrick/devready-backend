package br.com.devready.infrastructure.evaluation.persistence;

import br.com.devready.domain.evaluation.SubmissionStatus;
import java.time.Instant;
import java.util.UUID;

public record AttemptRow(
        UUID userId,
        UUID challengeId,
        SubmissionStatus status,
        Integer xpGained,
        Instant createdAt) {}
