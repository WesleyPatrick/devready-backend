package br.com.devready.application.evaluation.port;

import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SubmissionRepository {

    CodeSubmission save(CodeSubmission submission);

    Optional<CodeSubmission> findById(UUID id);

    boolean hasXpGained(UUID userId, UUID challengeId);

    List<CodeSubmission> findByUserAndChallenge(UUID userId, UUID challengeId, SubmissionKind kind);

    List<CodeSubmission> findBySession(UUID sessionId);
}
