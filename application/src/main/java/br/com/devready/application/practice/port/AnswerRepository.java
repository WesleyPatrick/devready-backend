package br.com.devready.application.practice.port;

import br.com.devready.domain.practice.Answer;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface AnswerRepository {
    Answer save(Answer answer);

    List<Answer> findByUser(UUID userId);

    List<Answer> findByUserAndQuestion(UUID userId, UUID questionId);

    List<Answer> findByQuestions(Collection<UUID> questionIds);
}
