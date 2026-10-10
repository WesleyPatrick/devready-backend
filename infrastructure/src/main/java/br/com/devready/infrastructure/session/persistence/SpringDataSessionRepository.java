package br.com.devready.infrastructure.session.persistence;

import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.session.SessionFormat;
import br.com.devready.domain.session.SessionStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSessionRepository extends JpaRepository<SessionEntity, UUID> {

    Optional<SessionEntity> findFirstByUserIdAndStatus(UUID userId, SessionStatus status);

    List<SessionEntity> findByUserIdOrderByStartedAtDescIdDesc(UUID userId);

    @Query("select s.id from SessionEntity s where s.status = :status")
    List<UUID> idsWithStatus(@Param("status") SessionStatus status);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value =
                    "update session_questions q set option_id = :option, correct = :correct"
                            + " where q.session_id = :session and q.position = :position"
                            + " and (:replace or q.option_id is null)"
                            + " and exists (select 1 from sessions s"
                            + " where s.id = :session and s.status = 'IN_PROGRESS' for share)",
            nativeQuery = true)
    int answer(
            @Param("session") UUID session,
            @Param("position") int position,
            @Param("option") UUID option,
            @Param("correct") boolean correct,
            @Param("replace") boolean replace);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value =
                    "update session_challenges c set submissions_used = c.submissions_used + 1"
                            + " where c.session_id = :session and c.position = :position"
                            + " and c.submissions_used < :max"
                            + " and exists (select 1 from sessions s"
                            + " where s.id = :session and s.status = 'IN_PROGRESS' for share)",
            nativeQuery = true)
    int useSubmission(
            @Param("session") UUID session,
            @Param("position") int position,
            @Param("max") int maxSubmissions);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update SessionEntity s set s.status = :to where s.id = :id and s.status = :from")
    int changeStatus(
            @Param("id") UUID id, @Param("from") SessionStatus from, @Param("to") SessionStatus to);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update SessionEntity s set s.status = :to"
                    + " where s.status = :from and s.deadlineAt < :cutoff")
    int expireDue(
            @Param("from") SessionStatus from,
            @Param("to") SessionStatus to,
            @Param("cutoff") Instant cutoff);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update SessionEntity s set s.status = :completed, s.finishedAt = :finishedAt,"
                    + " s.objectiveScore = :objective, s.challengeScore = :challenge,"
                    + " s.finalScore = :result, s.xpAwarded = :xp"
                    + " where s.id = :id and s.status = :submitted")
    int complete(
            @Param("id") UUID id,
            @Param("submitted") SessionStatus submitted,
            @Param("completed") SessionStatus completed,
            @Param("finishedAt") Instant finishedAt,
            @Param("objective") Double objective,
            @Param("challenge") Double challenge,
            @Param("result") Double result,
            @Param("xp") int xp);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            value =
                    "update session_challenges set best_submission_id = :best"
                            + " where session_id = :session and position = :position",
            nativeQuery = true)
    int setBest(
            @Param("session") UUID session,
            @Param("position") int position,
            @Param("best") UUID best);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update SessionEntity s set s.status = :failed, s.failureReason = :reason,"
                    + " s.finishedAt = :finishedAt where s.id = :id and s.status = :submitted")
    int fail(
            @Param("id") UUID id,
            @Param("submitted") SessionStatus submitted,
            @Param("failed") SessionStatus failed,
            @Param("reason") String reason,
            @Param("finishedAt") Instant finishedAt);

    @Query(
            "select new br.com.devready.infrastructure.session.persistence.SimulationRow("
                    + "s.finishedAt, s.xpAwarded) from SessionEntity s"
                    + " where s.userId = :userId and s.format = :format and s.status = :status"
                    + " order by s.finishedAt asc, s.id asc")
    List<SimulationRow> simulationsOf(
            @Param("userId") UUID userId,
            @Param("format") SessionFormat format,
            @Param("status") SessionStatus status);

    @Query(
            "select distinct q.questionId from SessionEntity s join s.questions q"
                    + " where s.userId = :userId and s.format = :format and s.status = :status"
                    + " and q.correct = true")
    List<UUID> questionsRightIn(
            @Param("userId") UUID userId,
            @Param("format") SessionFormat format,
            @Param("status") SessionStatus status);

    @Query(
            "select distinct c.challengeId from CodeSubmissionEntity c, SessionEntity s"
                    + " where c.sessionId = s.id and s.userId = :userId and s.status = :status"
                    + " and c.kind = :kind and c.status = :passed")
    List<UUID> challengesPassedIn(
            @Param("userId") UUID userId,
            @Param("status") SessionStatus status,
            @Param("kind") SubmissionKind kind,
            @Param("passed") SubmissionStatus passed);

    @Query(
            "select distinct qe.slug from SessionEntity s join s.questions q, QuestionEntity qe"
                    + " where qe.id = q.questionId and s.userId = :userId and s.format = :format"
                    + " and s.status = :status and q.correct = true")
    List<String> questionSlugsRightIn(
            @Param("userId") UUID userId,
            @Param("format") SessionFormat format,
            @Param("status") SessionStatus status);

    @Query(
            "select distinct ch.slug from CodeSubmissionEntity c, SessionEntity s, ChallengeEntity ch"
                    + " where c.sessionId = s.id and ch.id = c.challengeId and s.userId = :userId"
                    + " and s.status = :status and c.kind = :kind and c.status = :passed")
    List<String> challengeSlugsPassedIn(
            @Param("userId") UUID userId,
            @Param("status") SessionStatus status,
            @Param("kind") SubmissionKind kind,
            @Param("passed") SubmissionStatus passed);

    @Query(
            "select c.challengeId, s.id, s.status from SessionEntity s join s.challenges c"
                    + " where s.userId = :userId and s.format = :format and s.status in :statuses")
    List<Object[]> challengesInOpenSessions(
            @Param("userId") UUID userId,
            @Param("format") SessionFormat format,
            @Param("statuses") Collection<SessionStatus> statuses);

    @Query(
            "select q.questionId, s.id, s.status from SessionEntity s join s.questions q"
                    + " where s.userId = :userId and s.format = :format and s.status in :statuses")
    List<Object[]> questionsInOpenSessions(
            @Param("userId") UUID userId,
            @Param("format") SessionFormat format,
            @Param("statuses") Collection<SessionStatus> statuses);
}
