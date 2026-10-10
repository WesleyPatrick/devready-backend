package br.com.devready.application.practice.fake;

import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.domain.practice.Answer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

public class InMemoryAnswerRepository implements AnswerRepository {
    private final List<Answer> answers = new ArrayList<>();
    private Collection<UUID> lastQuestionQuery = List.of();

    @Override
    public Answer save(Answer answer) {
        answers.add(answer);
        return answer;
    }

    @Override
    public List<Answer> findByUser(UUID userId) {
        return oldestFirst(answer -> answer.userId().equals(userId));
    }

    @Override
    public List<Answer> findByUserAndQuestion(UUID userId, UUID questionId) {
        return oldestFirst(
                answer -> answer.userId().equals(userId) && answer.questionId().equals(questionId));
    }

    @Override
    public List<Answer> findByQuestions(Collection<UUID> questionIds) {
        lastQuestionQuery = List.copyOf(questionIds);
        return oldestFirst(answer -> questionIds.contains(answer.questionId()));
    }

    public List<Answer> all() {
        return List.copyOf(answers);
    }

    public Collection<UUID> lastQuestionQuery() {
        return lastQuestionQuery;
    }

    private List<Answer> oldestFirst(Predicate<Answer> filter) {
        return answers.stream()
                .filter(filter)
                .sorted(Comparator.comparing(Answer::answeredAt))
                .toList();
    }
}
