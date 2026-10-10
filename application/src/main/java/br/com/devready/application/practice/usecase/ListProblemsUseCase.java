package br.com.devready.application.practice.usecase;

import br.com.devready.application.practice.dto.PlacedProblem;
import br.com.devready.application.practice.dto.ProblemPage;
import br.com.devready.application.practice.dto.ProblemQuery;
import br.com.devready.application.practice.dto.ProblemView;
import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.application.practice.port.ChallengeAttemptReader;
import br.com.devready.application.practice.port.ExamLockReader;
import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.application.practice.service.ProblemBoard;
import br.com.devready.domain.catalog.ProblemType;
import br.com.devready.domain.practice.FirstAttempts;
import br.com.devready.domain.practice.QuestionStatus;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;

public class ListProblemsUseCase {

    public static final int MAX_PAGE_SIZE = 50;

    private final ProblemBoard board;
    private final AnswerRepository answers;
    private final ChallengeAttemptReader attempts;
    private final SimulationRecordReader simulations;
    private final ExamLockReader locks;

    public ListProblemsUseCase(
            ProblemBoard board,
            AnswerRepository answers,
            ChallengeAttemptReader attempts,
            SimulationRecordReader simulations,
            ExamLockReader locks) {
        this.board = board;
        this.answers = answers;
        this.attempts = attempts;
        this.simulations = simulations;
        this.locks = locks;
    }

    public ProblemPage execute(ProblemQuery query) {
        Map<UUID, QuestionStatus> questionStatuses =
                QuestionStatus.withSolved(
                        QuestionStatus.byQuestion(answers.findByUser(query.userId())),
                        simulations.questionsSolvedBy(query.userId()));
        Map<UUID, QuestionStatus> challengeStatuses =
                QuestionStatus.withSolved(
                        QuestionStatus.byChallenge(attempts.findByUser(query.userId())),
                        simulations.challengesSolvedBy(query.userId()));
        List<PlacedProblem> matching =
                board.problemsOf(query.userId()).stream()
                        .filter(
                                problem ->
                                        matches(
                                                problem,
                                                statusOf(
                                                        problem,
                                                        questionStatuses,
                                                        challengeStatuses),
                                                query))
                        .toList();
        int size = Math.clamp(query.size(), 1, MAX_PAGE_SIZE);
        int totalPages = Math.max(1, (matching.size() + size - 1) / size);
        int page = Math.clamp(query.page(), 1, totalPages);
        List<PlacedProblem> slice =
                matching.subList(
                        Math.min((page - 1) * size, matching.size()),
                        Math.min(page * size, matching.size()));
        Map<UUID, FirstAttempts> questionRates =
                FirstAttempts.byQuestion(
                        answers.findByQuestions(idsOf(slice, ProblemType.QUESTION)));
        Map<UUID, FirstAttempts> challengeRates =
                FirstAttempts.byChallenge(
                        attempts.findByChallenges(idsOf(slice, ProblemType.CHALLENGE)));
        Set<UUID> challengesInExam = locks.lockedChallenges(query.userId()).keySet();
        Set<UUID> questionsInExam = locks.lockedQuestions(query.userId()).keySet();
        List<ProblemView> items =
                slice.stream()
                        .map(
                                problem ->
                                        new ProblemView(
                                                problem.problem(),
                                                problem.competency(),
                                                problem.askedBy(),
                                                statusOf(
                                                        problem,
                                                        questionStatuses,
                                                        challengeStatuses),
                                                acceptanceOf(
                                                        problem,
                                                        problem.problem().type()
                                                                        == ProblemType.QUESTION
                                                                ? questionRates
                                                                : challengeRates),
                                                (problem.problem().type() == ProblemType.CHALLENGE
                                                                ? challengesInExam
                                                                : questionsInExam)
                                                        .contains(problem.problem().id())))
                        .toList();
        return new ProblemPage(items, page, totalPages, matching.size());
    }

    private static List<UUID> idsOf(List<PlacedProblem> slice, ProblemType type) {
        return slice.stream()
                .filter(p -> p.problem().type() == type)
                .map(p -> p.problem().id())
                .toList();
    }

    private static QuestionStatus statusOf(
            PlacedProblem problem,
            Map<UUID, QuestionStatus> questionStatuses,
            Map<UUID, QuestionStatus> challengeStatuses) {
        Map<UUID, QuestionStatus> statuses =
                problem.problem().type() == ProblemType.QUESTION
                        ? questionStatuses
                        : challengeStatuses;
        return statuses.getOrDefault(problem.problem().id(), QuestionStatus.NOT_STARTED);
    }

    private static Double acceptanceOf(PlacedProblem problem, Map<UUID, FirstAttempts> rates) {
        OptionalDouble rate =
                rates.getOrDefault(problem.problem().id(), new FirstAttempts(0, 0)).acceptance();
        return rate.isPresent() ? rate.getAsDouble() : null;
    }

    private static boolean matches(PlacedProblem problem, QuestionStatus status, ProblemQuery q) {
        return (q.type() == null || problem.problem().type() == q.type())
                && (q.status() == null || status == q.status())
                && (q.difficulty() == null || problem.problem().difficulty() == q.difficulty())
                && (q.competency() == null
                        || (problem.competency() != null
                                && problem.competency().slug().equals(q.competency())))
                && (q.company() == null || problem.askedBy().contains(q.company()))
                && (q.level() == null || problem.problem().level() == q.level());
    }
}
