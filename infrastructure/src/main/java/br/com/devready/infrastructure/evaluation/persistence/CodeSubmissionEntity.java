package br.com.devready.infrastructure.evaluation.persistence;

import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "code_submissions")
public class CodeSubmissionEntity {

    @Id private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "challenge_id", nullable = false)
    private UUID challengeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Language language;

    @Column(name = "source_code", nullable = false, columnDefinition = "text")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SubmissionKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubmissionStatus status;

    @Column(name = "passed_count", nullable = false)
    private int passedCount;

    @Column(name = "total_count", nullable = false)
    private int totalCount;

    @Column(name = "compile_output", columnDefinition = "text")
    private String compileOutput;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "xp_reward", nullable = false)
    private int xpReward;

    @Column(name = "xp_gained", nullable = false)
    private int xpGained;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "session_id")
    private UUID sessionId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "submission_results", joinColumns = @JoinColumn(name = "submission_id"))
    @BatchSize(size = 20)
    private List<SubmissionResultEmbeddable> results = new ArrayList<>();

    protected CodeSubmissionEntity() {}

    CodeSubmissionEntity(UUID id) {
        this.id = id;
    }

    boolean isFinished() {
        return status != null && status.finished();
    }

    void apply(CodeSubmission submission) {
        userId = submission.userId();
        challengeId = submission.challengeId();
        language = submission.language();
        sourceCode = submission.sourceCode();
        kind = submission.kind();
        status = submission.status();
        passedCount = submission.passedCount();
        totalCount = submission.totalCount();
        compileOutput = submission.compileOutput();
        errorMessage = submission.errorMessage();
        xpReward = submission.xpReward();
        xpGained = submission.xpGained();
        createdAt = submission.createdAt();
        finishedAt = submission.finishedAt();
        sessionId = submission.sessionId();
        results.clear();
        submission
                .results()
                .forEach(result -> results.add(SubmissionResultEmbeddable.from(result)));
    }

    CodeSubmission toDomain() {
        return new CodeSubmission(
                id,
                userId,
                challengeId,
                language,
                sourceCode,
                kind,
                status,
                passedCount,
                totalCount,
                compileOutput,
                errorMessage,
                xpReward,
                xpGained,
                createdAt,
                finishedAt,
                results.stream()
                        .sorted(Comparator.comparingInt(SubmissionResultEmbeddable::position))
                        .map(SubmissionResultEmbeddable::toDomain)
                        .toList(),
                sessionId);
    }
}
