package br.com.devready.infrastructure.evaluation.persistence;

import br.com.devready.domain.evaluation.SubmissionKind;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataCodeSubmissionRepository
        extends JpaRepository<CodeSubmissionEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from CodeSubmissionEntity s where s.id = :id")
    Optional<CodeSubmissionEntity> findForUpdateById(@Param("id") UUID id);

    boolean existsByUserIdAndChallengeIdAndXpGainedGreaterThan(
            UUID userId, UUID challengeId, int xpGained);

    List<CodeSubmissionEntity>
            findByUserIdAndChallengeIdAndKindAndSessionIdIsNullOrderByCreatedAtDescIdDesc(
                    UUID userId, UUID challengeId, SubmissionKind kind);

    List<CodeSubmissionEntity> findBySessionIdOrderByCreatedAtAscIdAsc(UUID sessionId);

    @Query(
            "select new br.com.devready.infrastructure.evaluation.persistence.AttemptRow("
                    + "s.userId, s.challengeId, s.status, s.xpGained, s.createdAt)"
                    + " from CodeSubmissionEntity s"
                    + " where s.userId = :userId and s.kind = :kind and s.sessionId is null"
                    + " order by s.createdAt asc, s.id asc")
    List<AttemptRow> attemptsOfUser(
            @Param("userId") UUID userId, @Param("kind") SubmissionKind kind);

    @Query(
            "select new br.com.devready.infrastructure.evaluation.persistence.AttemptRow("
                    + "s.userId, s.challengeId, s.status, s.xpGained, s.createdAt)"
                    + " from CodeSubmissionEntity s"
                    + " where s.challengeId in :challengeIds and s.kind = :kind"
                    + " and s.sessionId is null"
                    + " order by s.createdAt asc, s.id asc")
    List<AttemptRow> attemptsOnChallenges(
            @Param("challengeIds") Collection<UUID> challengeIds,
            @Param("kind") SubmissionKind kind);

    @Query(
            value =
                    "select ch.slug, max(case when s.status = 'PASSED' then 1 else 0 end)"
                            + " from code_submissions s join challenges ch on ch.id = s.challenge_id"
                            + " where s.user_id = :userId and s.kind = 'SUBMIT'"
                            + " and s.session_id is null"
                            + " and s.status in ('PASSED', 'FAILED', 'COMPILE_ERROR',"
                            + " 'RUNTIME_ERROR', 'TIME_LIMIT')"
                            + " group by ch.slug",
            nativeQuery = true)
    List<Object[]> solvedChallengesBySlug(@Param("userId") UUID userId);
}
