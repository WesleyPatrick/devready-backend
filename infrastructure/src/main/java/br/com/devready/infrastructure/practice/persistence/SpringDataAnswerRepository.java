package br.com.devready.infrastructure.practice.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataAnswerRepository extends JpaRepository<AnswerEntity, UUID> {
    List<AnswerEntity> findByUserIdOrderByAnsweredAtAscIdAsc(UUID userId);

    List<AnswerEntity> findByUserIdAndQuestionIdOrderByAnsweredAtAscIdAsc(
            UUID userId, UUID questionId);

    List<AnswerEntity> findByQuestionIdInOrderByAnsweredAtAscIdAsc(Collection<UUID> questionIds);

    @Query(
            "select q.slug, max(case when a.correct = true then 1 else 0 end)"
                    + " from AnswerEntity a join QuestionEntity q on q.id = a.questionId"
                    + " where a.userId = :userId group by q.slug")
    List<Object[]> solvedBySlug(@Param("userId") UUID userId);
}
