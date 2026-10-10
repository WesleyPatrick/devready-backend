package br.com.devready.infrastructure.evaluation.persistence;

import br.com.devready.application.evaluation.exception.XpAlreadyGrantedException;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaSubmissionRepository implements SubmissionRepository {

    static final String FIRST_SOLVE_INDEX = "code_submissions_first_solve_idx";

    private final SpringDataCodeSubmissionRepository submissions;

    public JpaSubmissionRepository(SpringDataCodeSubmissionRepository submissions) {
        this.submissions = submissions;
    }

    @Override
    public CodeSubmission save(CodeSubmission submission) {
        CodeSubmissionEntity entity =
                submissions
                        .findForUpdateById(submission.id())
                        .orElseGet(() -> new CodeSubmissionEntity(submission.id()));
        if (entity.isFinished()) {
            return entity.toDomain();
        }
        entity.apply(submission);
        try {
            submissions.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            if (mentions(e, FIRST_SOLVE_INDEX)) {
                throw new XpAlreadyGrantedException();
            }
            throw e;
        }
        return submission;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CodeSubmission> findById(UUID id) {
        return submissions.findById(id).map(CodeSubmissionEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasXpGained(UUID userId, UUID challengeId) {
        return submissions.existsByUserIdAndChallengeIdAndXpGainedGreaterThan(
                userId, challengeId, 0);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodeSubmission> findByUserAndChallenge(
            UUID userId, UUID challengeId, SubmissionKind kind) {
        return submissions
                .findByUserIdAndChallengeIdAndKindAndSessionIdIsNullOrderByCreatedAtDescIdDesc(
                        userId, challengeId, kind)
                .stream()
                .map(CodeSubmissionEntity::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CodeSubmission> findBySession(UUID sessionId) {
        return submissions.findBySessionIdOrderByCreatedAtAscIdAsc(sessionId).stream()
                .map(CodeSubmissionEntity::toDomain)
                .toList();
    }

    private static boolean mentions(Throwable error, String constraint) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains(constraint)) {
                return true;
            }
        }
        return false;
    }
}
