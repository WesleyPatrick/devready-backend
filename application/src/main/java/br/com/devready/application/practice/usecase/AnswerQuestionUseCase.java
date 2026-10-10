package br.com.devready.application.practice.usecase;

import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.application.practice.dto.AnswerResult;
import br.com.devready.application.practice.exception.OptionNotInQuestionException;
import br.com.devready.application.practice.exception.QuestionNotFoundException;
import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.application.practice.port.ExamLockReader;
import br.com.devready.application.practice.service.ExamGuard;
import br.com.devready.application.practice.service.VisibleLevels;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import br.com.devready.domain.practice.Answer;
import br.com.devready.domain.practice.Xp;
import java.util.UUID;

public class AnswerQuestionUseCase {

    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final ExamLockReader locks;
    private final VisibleLevels levels;
    private final IdGenerator ids;
    private final Clock clock;

    public AnswerQuestionUseCase(
            QuestionRepository questions,
            AnswerRepository answers,
            ExamLockReader locks,
            VisibleLevels levels,
            IdGenerator ids,
            Clock clock) {
        this.questions = questions;
        this.answers = answers;
        this.locks = locks;
        this.levels = levels;
        this.ids = ids;
        this.clock = clock;
    }

    public AnswerResult execute(UUID userId, String questionSlug, UUID optionId) {
        Question question = levels.question(userId, activeQuestion(questionSlug));
        ExamGuard.requireQuestionFree(locks, userId, question.id());
        return save(userId, question, optionId);
    }

    public AnswerResult record(UUID userId, String questionSlug, UUID optionId) {
        return save(userId, activeQuestion(questionSlug), optionId);
    }

    private Question activeQuestion(String slug) {
        return questions
                .findBySlug(slug)
                .filter(Question::active)
                .orElseThrow(() -> new QuestionNotFoundException(slug));
    }

    private AnswerResult save(UUID userId, Question question, UUID optionId) {
        QuestionOption option =
                question.options().stream()
                        .filter(candidate -> candidate.id().equals(optionId))
                        .findFirst()
                        .orElseThrow(OptionNotInQuestionException::new);
        boolean solvedBefore =
                answers.findByUserAndQuestion(userId, question.id()).stream()
                        .anyMatch(Answer::correct);
        int xp = Xp.gained(question.difficulty(), option.correct(), solvedBefore);
        answers.save(
                new Answer(
                        ids.next(),
                        userId,
                        question.id(),
                        option.id(),
                        option.correct(),
                        xp,
                        clock.now()));
        return new AnswerResult(
                option.correct(), question.correctOption().id(), question.explanation(), xp);
    }
}
