package br.com.devready.application.session.fake;

import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.evaluation.fake.RecordingSubmissionQueue;
import br.com.devready.application.evaluation.fake.SubmissionFixtures;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.practice.usecase.AnswerQuestionUseCase;
import br.com.devready.application.session.service.SessionOpener;
import br.com.devready.application.session.usecase.FinalizeSessionUseCase;
import br.com.devready.application.session.usecase.FinishSessionUseCase;
import br.com.devready.application.shared.DirectTransactions;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import br.com.devready.domain.session.RandomSource;
import br.com.devready.domain.session.Session;
import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class SessionWorld {

    public static final Instant T0 = SubmissionFixtures.T0;

    public final PracticeWorld practice = new PracticeWorld();
    public final UUID user = practice.user;
    public final InMemorySessionRepository sessions = new InMemorySessionRepository();
    public final InMemorySessionPool pool =
            new InMemorySessionPool(practice.areas, practice.questions, practice.challenges);
    public final RecordingSubmissionQueue queue = new RecordingSubmissionQueue();
    public final FixedClock clock = new FixedClock(T0);
    public final SequentialIdGenerator ids = new SequentialIdGenerator();
    public final DirectTransactions transactions = new DirectTransactions();
    public final RandomSource random = new Random(7)::nextInt;
    public final SubmissionDesk desk =
            new SubmissionDesk(practice.challenges, practice.submissions, queue, ids, clock);
    public final AnswerQuestionUseCase answerQuestion =
            new AnswerQuestionUseCase(
                    practice.questions,
                    practice.answers,
                    practice.examLocks,
                    practice.levels,
                    ids,
                    clock);
    public final FinalizeSessionUseCase finalizeSession =
            new FinalizeSessionUseCase(sessions, practice.submissions, desk, clock);
    public final FinishSessionUseCase finishSession =
            new FinishSessionUseCase(sessions, finalizeSession);
    public final SessionOpener opener = new SessionOpener(sessions, finishSession);

    public SessionWorld() {
        practice.challenges.save(
                CatalogFixtures.challenge(
                        "c-two", "Dois números", backend(), Difficulty.EASY, "sql"));
    }

    public Area backend() {
        return practice.areas.findBySlug("backend").orElseThrow();
    }

    public Session openSimulation() {
        List<UUID> questions =
                pool.activeQuestions(backend().id(), Level.JUNIOR).stream()
                        .map(Question::id)
                        .sorted()
                        .toList();
        List<UUID> challenges =
                pool.activeChallenges(backend().id(), Level.JUNIOR).stream()
                        .map(Challenge::id)
                        .sorted()
                        .toList();
        return sessions.create(
                Session.simulation(
                        ids.next(),
                        user,
                        backend().id(),
                        Level.JUNIOR,
                        backend().sessionConfig(),
                        questions,
                        challenges,
                        clock.now()));
    }

    public Session openLesson() {
        return sessions.create(
                Session.lesson(
                        ids.next(),
                        user,
                        backend().id(),
                        Level.JUNIOR,
                        "sql",
                        List.of(id("q-sql-easy"), id("q-sql-hard"), id("q-java-sql")),
                        clock.now()));
    }

    public UUID id(String questionSlug) {
        return practice.question(questionSlug).id();
    }

    public int position(Session session, String questionSlug) {
        UUID wanted = id(questionSlug);
        return session.questions().stream()
                .filter(q -> q.questionId().equals(wanted))
                .findFirst()
                .orElseThrow()
                .position();
    }

    public int challengePosition(Session session, String challengeSlug) {
        UUID wanted = practice.challenges.get(challengeSlug).id();
        return session.challenges().stream()
                .filter(c -> c.challengeId().equals(wanted))
                .findFirst()
                .orElseThrow()
                .position();
    }

    public UUID option(String questionSlug, boolean correct) {
        return practice.question(questionSlug).options().stream()
                .filter(o -> o.correct() == correct)
                .map(QuestionOption::id)
                .findFirst()
                .orElseThrow();
    }
}
