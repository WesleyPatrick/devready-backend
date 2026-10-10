package br.com.devready.infrastructure.config;

import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.evaluation.port.LanguageRuntimes;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.application.practice.service.ProblemBoard;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.practice.usecase.AnswerQuestionUseCase;
import br.com.devready.application.practice.usecase.GetSubmissionUseCase;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.application.session.service.SessionOpener;
import br.com.devready.application.session.service.SessionViews;
import br.com.devready.application.session.usecase.AnswerSessionQuestionUseCase;
import br.com.devready.application.session.usecase.ExpireSessionsUseCase;
import br.com.devready.application.session.usecase.FinalizeSessionUseCase;
import br.com.devready.application.session.usecase.FinishSessionUseCase;
import br.com.devready.application.session.usecase.GetCurrentSessionUseCase;
import br.com.devready.application.session.usecase.GetSessionResultUseCase;
import br.com.devready.application.session.usecase.GetSessionUseCase;
import br.com.devready.application.session.usecase.ListSessionsUseCase;
import br.com.devready.application.session.usecase.RunSessionCodeUseCase;
import br.com.devready.application.session.usecase.StartLessonUseCase;
import br.com.devready.application.session.usecase.StartSimulationUseCase;
import br.com.devready.application.session.usecase.SubmitSessionCodeUseCase;
import br.com.devready.application.session.usecase.ViewSubmissionUseCase;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.application.shared.Transactions;
import br.com.devready.domain.session.RandomSource;
import br.com.devready.infrastructure.shared.SpringTransactions;
import br.com.devready.infrastructure.shared.SystemRandomSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SessionConfiguration {

    @Bean
    Transactions transactions(PlatformTransactionManager transactionManager) {
        return new SpringTransactions(transactionManager);
    }

    @Bean
    RandomSource randomSource() {
        return new SystemRandomSource();
    }

    @Bean
    FinalizeSessionUseCase finalizeSession(
            SessionRepository sessions,
            SubmissionRepository submissions,
            SubmissionDesk desk,
            Clock clock) {
        return new FinalizeSessionUseCase(sessions, submissions, desk, clock);
    }

    @Bean
    FinishSessionUseCase finishSession(
            SessionRepository sessions, FinalizeSessionUseCase finalizeSession) {
        return new FinishSessionUseCase(sessions, finalizeSession);
    }

    @Bean
    SessionOpener sessionOpener(SessionRepository sessions, FinishSessionUseCase finishSession) {
        return new SessionOpener(sessions, finishSession);
    }

    @Bean
    StartLessonUseCase startLesson(
            CareerProfileRepository profiles,
            TrackAssembler assembler,
            AreaRepository areas,
            SessionPool pool,
            AnswerRepository answers,
            SimulationRecordReader simulations,
            SessionOpener opener,
            RandomSource random,
            IdGenerator ids,
            Clock clock) {
        return new StartLessonUseCase(
                profiles, assembler, areas, pool, answers, simulations, opener, random, ids, clock);
    }

    @Bean
    StartSimulationUseCase startSimulation(
            CareerProfileRepository profiles,
            AreaRepository areas,
            SessionPool pool,
            SessionOpener opener,
            RandomSource random,
            IdGenerator ids,
            Clock clock) {
        return new StartSimulationUseCase(profiles, areas, pool, opener, random, ids, clock);
    }

    @Bean
    AnswerSessionQuestionUseCase answerSessionQuestion(
            SessionRepository sessions,
            SessionPool pool,
            AnswerQuestionUseCase answerQuestion,
            FinishSessionUseCase finishSession,
            Transactions transactions,
            Clock clock) {
        return new AnswerSessionQuestionUseCase(
                sessions, pool, answerQuestion, finishSession, transactions, clock);
    }

    @Bean
    RunSessionCodeUseCase runSessionCode(
            SessionRepository sessions, SessionPool pool, SubmissionDesk desk, Clock clock) {
        return new RunSessionCodeUseCase(sessions, pool, desk, clock);
    }

    @Bean
    SubmitSessionCodeUseCase submitSessionCode(
            SessionRepository sessions,
            SessionPool pool,
            SubmissionDesk desk,
            Transactions transactions,
            Clock clock) {
        return new SubmitSessionCodeUseCase(sessions, pool, desk, transactions, clock);
    }

    @Bean
    SessionViews sessionViews(
            SessionPool pool,
            SubmissionRepository submissions,
            CompetencyRepository competencies,
            LanguageRuntimes runtimes) {
        return new SessionViews(pool, submissions, competencies, runtimes);
    }

    @Bean
    GetSessionUseCase getSession(SessionRepository sessions, SessionViews views) {
        return new GetSessionUseCase(sessions, views);
    }

    @Bean
    GetCurrentSessionUseCase getCurrentSession(SessionRepository sessions, SessionViews views) {
        return new GetCurrentSessionUseCase(sessions, views);
    }

    @Bean
    ListSessionsUseCase listSessions(SessionRepository sessions, SessionViews views) {
        return new ListSessionsUseCase(sessions, views);
    }

    @Bean
    GetSessionResultUseCase getSessionResult(
            SessionRepository sessions,
            SessionPool pool,
            SubmissionRepository submissions,
            ProblemBoard board) {
        return new GetSessionResultUseCase(sessions, pool, submissions, board);
    }

    @Bean
    ExpireSessionsUseCase expireSessions(
            SessionRepository sessions, FinalizeSessionUseCase finalizeSession, Clock clock) {
        return new ExpireSessionsUseCase(sessions, finalizeSession, clock);
    }

    @Bean
    ViewSubmissionUseCase viewSubmission(
            GetSubmissionUseCase getSubmission, SessionRepository sessions) {
        return new ViewSubmissionUseCase(getSubmission, sessions);
    }
}
