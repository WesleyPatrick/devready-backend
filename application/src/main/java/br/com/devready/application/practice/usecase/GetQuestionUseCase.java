package br.com.devready.application.practice.usecase;

import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.application.practice.dto.QuestionView;
import br.com.devready.application.practice.exception.QuestionNotFoundException;
import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.application.practice.port.ExamLockReader;
import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.application.practice.service.ExamGuard;
import br.com.devready.application.practice.service.ProblemBoard;
import br.com.devready.application.practice.service.VisibleLevels;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.practice.QuestionStatus;
import java.util.UUID;

public class GetQuestionUseCase {

    private final QuestionRepository questions;
    private final ProblemBoard board;
    private final AnswerRepository answers;
    private final SimulationRecordReader simulations;
    private final ExamLockReader locks;
    private final VisibleLevels levels;

    public GetQuestionUseCase(
            QuestionRepository questions,
            ProblemBoard board,
            AnswerRepository answers,
            SimulationRecordReader simulations,
            ExamLockReader locks,
            VisibleLevels levels) {
        this.questions = questions;
        this.board = board;
        this.answers = answers;
        this.simulations = simulations;
        this.locks = locks;
        this.levels = levels;
    }

    public QuestionView execute(UUID userId, String slug) {
        Question question =
                levels.question(
                        userId,
                        questions
                                .findBySlug(slug)
                                .filter(Question::active)
                                .orElseThrow(() -> new QuestionNotFoundException(slug)));
        ExamGuard.requireQuestionFree(locks, userId, question.id());
        QuestionStatus status =
                simulations.questionsSolvedBy(userId).contains(question.id())
                        ? QuestionStatus.SOLVED
                        : QuestionStatus.of(answers.findByUserAndQuestion(userId, question.id()));
        return QuestionView.of(question, board.competencyOf(userId, question.tagSlugs()), status);
    }
}
