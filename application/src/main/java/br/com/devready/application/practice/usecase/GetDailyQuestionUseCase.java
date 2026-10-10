package br.com.devready.application.practice.usecase;

import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.application.practice.dto.PlacedProblem;
import br.com.devready.application.practice.dto.QuestionView;
import br.com.devready.application.practice.exception.NoDailyQuestionException;
import br.com.devready.application.practice.exception.QuestionNotFoundException;
import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.application.practice.service.ProblemBoard;
import br.com.devready.application.shared.Clock;
import br.com.devready.domain.catalog.ProblemType;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.practice.Answer;
import br.com.devready.domain.practice.DailyQuestion;
import br.com.devready.domain.practice.QuestionStatus;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class GetDailyQuestionUseCase {

    private final ProblemBoard board;
    private final QuestionRepository questions;
    private final AnswerRepository answers;
    private final SimulationRecordReader simulations;
    private final Clock clock;

    public GetDailyQuestionUseCase(
            ProblemBoard board,
            QuestionRepository questions,
            AnswerRepository answers,
            SimulationRecordReader simulations,
            Clock clock) {
        this.board = board;
        this.questions = questions;
        this.answers = answers;
        this.simulations = simulations;
        this.clock = clock;
    }

    public QuestionView execute(UUID userId) {
        List<PlacedProblem> trackQuestions =
                board.problemsOf(userId).stream()
                        .filter(p -> p.inTrack() && p.problem().type() == ProblemType.QUESTION)
                        .toList();
        List<Answer> mine = answers.findByUser(userId);
        Set<UUID> solvedInSimulations = simulations.questionsSolvedBy(userId);
        UUID chosenId =
                DailyQuestion.pick(
                                trackQuestions.stream().map(p -> p.problem().id()).toList(),
                                mine,
                                solvedInSimulations,
                                clock.now())
                        .orElseThrow(NoDailyQuestionException::new);
        PlacedProblem chosen =
                trackQuestions.stream()
                        .filter(p -> p.problem().id().equals(chosenId))
                        .findFirst()
                        .orElseThrow();
        String slug = chosen.problem().slug();
        Question question =
                questions.findBySlug(slug).orElseThrow(() -> new QuestionNotFoundException(slug));
        List<Answer> toThisQuestion =
                mine.stream().filter(answer -> answer.questionId().equals(question.id())).toList();
        QuestionStatus status =
                solvedInSimulations.contains(question.id())
                        ? QuestionStatus.SOLVED
                        : QuestionStatus.of(toThisQuestion);
        return QuestionView.of(question, chosen.competency(), status);
    }
}
