package br.com.devready.infrastructure.config;

import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.catalog.port.PracticeCatalog;
import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.application.evaluation.port.LanguageRuntimes;
import br.com.devready.application.evaluation.port.SubmissionQueue;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.application.practice.port.ChallengeAttemptReader;
import br.com.devready.application.practice.port.ExamLockReader;
import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.application.practice.service.ProblemBoard;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.practice.service.VisibleLevels;
import br.com.devready.application.practice.usecase.AnswerQuestionUseCase;
import br.com.devready.application.practice.usecase.GetChallengeUseCase;
import br.com.devready.application.practice.usecase.GetDailyQuestionUseCase;
import br.com.devready.application.practice.usecase.GetProgressUseCase;
import br.com.devready.application.practice.usecase.GetQuestionUseCase;
import br.com.devready.application.practice.usecase.GetSubmissionUseCase;
import br.com.devready.application.practice.usecase.ListChallengeSubmissionsUseCase;
import br.com.devready.application.practice.usecase.ListProblemsUseCase;
import br.com.devready.application.practice.usecase.RunCodeUseCase;
import br.com.devready.application.practice.usecase.SubmitCodeUseCase;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PracticeConfiguration {

    @Bean
    ProblemBoard problemBoard(
            TrackAssembler assembler,
            AreaRepository areas,
            PracticeCatalog catalog,
            CompetencyRepository competencies) {
        return new ProblemBoard(assembler, areas, catalog, competencies);
    }

    @Bean
    VisibleLevels visibleLevels(CareerProfileRepository profiles) {
        return new VisibleLevels(profiles);
    }

    @Bean
    AnswerQuestionUseCase answerQuestion(
            QuestionRepository questions,
            AnswerRepository answers,
            ExamLockReader locks,
            VisibleLevels levels,
            IdGenerator ids,
            Clock clock) {
        return new AnswerQuestionUseCase(questions, answers, locks, levels, ids, clock);
    }

    @Bean
    GetDailyQuestionUseCase getDailyQuestion(
            ProblemBoard board,
            QuestionRepository questions,
            AnswerRepository answers,
            SimulationRecordReader simulations,
            Clock clock) {
        return new GetDailyQuestionUseCase(board, questions, answers, simulations, clock);
    }

    @Bean
    SubmissionDesk submissionDesk(
            ChallengeRepository challenges,
            SubmissionRepository submissions,
            SubmissionQueue queue,
            IdGenerator ids,
            Clock clock) {
        return new SubmissionDesk(challenges, submissions, queue, ids, clock);
    }

    @Bean
    GetSubmissionUseCase getSubmission(
            SubmissionRepository submissions, ChallengeRepository challenges, SubmissionDesk desk) {
        return new GetSubmissionUseCase(submissions, challenges, desk);
    }

    @Bean
    ListChallengeSubmissionsUseCase listChallengeSubmissions(
            SubmissionDesk desk,
            SubmissionRepository submissions,
            ExamLockReader locks,
            VisibleLevels levels) {
        return new ListChallengeSubmissionsUseCase(desk, submissions, locks, levels);
    }

    @Bean
    ListProblemsUseCase listProblems(
            ProblemBoard board,
            AnswerRepository answers,
            ChallengeAttemptReader attempts,
            SimulationRecordReader simulations,
            ExamLockReader locks) {
        return new ListProblemsUseCase(board, answers, attempts, simulations, locks);
    }

    @Bean
    GetQuestionUseCase getQuestion(
            QuestionRepository questions,
            ProblemBoard board,
            AnswerRepository answers,
            SimulationRecordReader simulations,
            ExamLockReader locks,
            VisibleLevels levels) {
        return new GetQuestionUseCase(questions, board, answers, simulations, locks, levels);
    }

    @Bean
    GetProgressUseCase getProgress(
            AnswerRepository answers,
            ChallengeAttemptReader attempts,
            SimulationRecordReader simulations,
            Clock clock) {
        return new GetProgressUseCase(answers, attempts, simulations, clock);
    }

    @Bean
    GetChallengeUseCase getChallenge(
            SubmissionDesk desk,
            ProblemBoard board,
            ChallengeAttemptReader attempts,
            SimulationRecordReader simulations,
            ExamLockReader locks,
            LanguageRuntimes runtimes,
            VisibleLevels levels) {
        return new GetChallengeUseCase(desk, board, attempts, simulations, locks, runtimes, levels);
    }

    @Bean
    RunCodeUseCase runCode(SubmissionDesk desk, ExamLockReader locks, VisibleLevels levels) {
        return new RunCodeUseCase(desk, locks, levels);
    }

    @Bean
    SubmitCodeUseCase submitCode(SubmissionDesk desk, ExamLockReader locks, VisibleLevels levels) {
        return new SubmitCodeUseCase(desk, locks, levels);
    }
}
