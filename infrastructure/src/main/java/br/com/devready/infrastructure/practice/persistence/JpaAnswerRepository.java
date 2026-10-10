package br.com.devready.infrastructure.practice.persistence;

import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.domain.practice.Answer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaAnswerRepository implements AnswerRepository {

    private final SpringDataAnswerRepository answers;

    public JpaAnswerRepository(SpringDataAnswerRepository answers) {
        this.answers = answers;
    }

    @Override
    public Answer save(Answer answer) {
        answers.saveAndFlush(
                new AnswerEntity(
                        answer.id(),
                        answer.userId(),
                        answer.questionId(),
                        answer.optionId(),
                        answer.correct(),
                        answer.xpGained(),
                        answer.answeredAt()));
        return answer;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Answer> findByUser(UUID userId) {
        return answers.findByUserIdOrderByAnsweredAtAscIdAsc(userId).stream()
                .map(JpaAnswerRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Answer> findByUserAndQuestion(UUID userId, UUID questionId) {
        return answers
                .findByUserIdAndQuestionIdOrderByAnsweredAtAscIdAsc(userId, questionId)
                .stream()
                .map(JpaAnswerRepository::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Answer> findByQuestions(Collection<UUID> questionIds) {
        if (questionIds.isEmpty()) {
            return List.of();
        }
        return answers.findByQuestionIdInOrderByAnsweredAtAscIdAsc(questionIds).stream()
                .map(JpaAnswerRepository::toDomain)
                .toList();
    }

    private static Answer toDomain(AnswerEntity e) {
        return new Answer(
                e.getId(),
                e.getUserId(),
                e.getQuestionId(),
                e.getOptionId(),
                e.isCorrect(),
                e.getXpGained(),
                e.getAnsweredAt());
    }
}
