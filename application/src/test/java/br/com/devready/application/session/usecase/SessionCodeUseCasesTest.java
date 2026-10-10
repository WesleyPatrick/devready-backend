package br.com.devready.application.session.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.evaluation.fake.FixedLanguageRuntimes;
import br.com.devready.application.practice.dto.SubmissionTicket;
import br.com.devready.application.practice.exception.LanguageNotOfferedException;
import br.com.devready.application.session.exception.ItemNotInSessionException;
import br.com.devready.application.session.exception.NoSubmissionsLeftException;
import br.com.devready.application.session.exception.SessionClosedException;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.application.session.service.SessionViews;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.session.Session;
import java.util.List;
import org.junit.jupiter.api.Test;

class SessionCodeUseCasesTest {

    private final SessionWorld world = new SessionWorld();
    private final RunSessionCodeUseCase run =
            new RunSessionCodeUseCase(world.sessions, world.pool, world.desk, world.clock);
    private final SubmitSessionCodeUseCase submit =
            new SubmitSessionCodeUseCase(
                    world.sessions, world.pool, world.desk, world.transactions, world.clock);
    private final GetSessionUseCase get =
            new GetSessionUseCase(
                    world.sessions,
                    new SessionViews(
                            world.pool,
                            world.practice.submissions,
                            world.practice.competencies,
                            new FixedLanguageRuntimes()));

    @Test
    void runningGoesToTheQueueWithTheSessionAndSpendsNothing() {
        Session session = world.openSimulation();
        int position = world.challengePosition(session, "c-sql");

        SubmissionTicket ticket =
                run.execute(world.user, session.id(), position, Language.JAVA, "x");

        CodeSubmission saved = world.practice.submissions.findById(ticket.id()).orElseThrow();
        assertThat(saved.kind()).isEqualTo(SubmissionKind.RUN);
        assertThat(saved.sessionId()).isEqualTo(session.id());
        assertThat(saved.totalCount()).isEqualTo(2);
        assertThat(
                        world.sessions
                                .get(session.id())
                                .challenge(position)
                                .orElseThrow()
                                .submissionsUsed())
                .isZero();
        assertThat(world.queue.published()).containsExactly(ticket.id());
    }

    @Test
    void eachSubmissionSpendsOneOfFiveAndTheSixthIsRefused() {
        Session session = world.openSimulation();
        int position = world.challengePosition(session, "c-sql");
        for (int i = 0; i < 5; i++) {
            submit.execute(world.user, session.id(), position, Language.JAVA, "x" + i);
        }

        assertThatThrownBy(
                        () ->
                                submit.execute(
                                        world.user, session.id(), position, Language.JAVA, "y"))
                .isInstanceOf(NoSubmissionsLeftException.class)
                .hasMessage("Os 5 envios do desafio " + position + " já foram usados.");
        assertThat(world.practice.submissions.findBySession(session.id())).hasSize(5);
        assertThat(world.practice.submissions.findBySession(session.id()))
                .allMatch(s -> s.kind() == SubmissionKind.SUBMIT && s.xpReward() == 0);
        assertThat(world.transactions.calls()).isEqualTo(5);
    }

    @Test
    void theLimitIsTheOneOfTheChallenge() {
        Challenge sql = world.practice.challenges.get("c-sql");
        world.practice.challenges.save(withMaxSubmissions(sql, 2));
        Session session = world.openSimulation();
        int position = world.challengePosition(session, "c-sql");
        submit.execute(world.user, session.id(), position, Language.JAVA, "a");

        assertThat(
                        get.execute(world.user, session.id())
                                .challenges()
                                .get(position - 1)
                                .submissionsLeft())
                .isEqualTo(1);
        submit.execute(world.user, session.id(), position, Language.JAVA, "b");
        assertThatThrownBy(
                        () ->
                                submit.execute(
                                        world.user, session.id(), position, Language.JAVA, "c"))
                .isInstanceOf(NoSubmissionsLeftException.class)
                .hasMessage("Os 2 envios do desafio " + position + " já foram usados.");
        assertThat(world.practice.submissions.findBySession(session.id())).hasSize(2);
    }

    private static Challenge withMaxSubmissions(Challenge c, int max) {
        return new Challenge(
                c.id(),
                c.slug(),
                c.title(),
                c.areaId(),
                c.level(),
                c.difficulty(),
                c.statement(),
                c.approachSummary(),
                c.limits(),
                c.comparisonMode(),
                max,
                c.languages(),
                c.testCases(),
                c.tagSlugs(),
                c.active());
    }

    @Test
    void aLanguageTheChallengeDoesNotOfferSpendsNothing() {
        world.practice.challenges.save(
                CatalogFixtures.challenge(
                        "c-two",
                        "Dois números",
                        world.backend(),
                        Difficulty.EASY,
                        true,
                        List.of(Language.JAVA),
                        "sql"));
        Session session = world.openSimulation();
        int position = world.challengePosition(session, "c-two");

        assertThatThrownBy(
                        () ->
                                submit.execute(
                                        world.user, session.id(), position, Language.PYTHON, "x"))
                .isInstanceOf(LanguageNotOfferedException.class);
        assertThat(
                        world.sessions
                                .get(session.id())
                                .challenge(position)
                                .orElseThrow()
                                .submissionsUsed())
                .isZero();
    }

    @Test
    void afterTheDeadlineOrOutsideTheSessionNothingRuns() {
        Session session = world.openSimulation();
        world.clock.advanceTo(session.deadlineAt().plusSeconds(31));

        assertThatThrownBy(() -> run.execute(world.user, session.id(), 1, Language.JAVA, "x"))
                .isInstanceOf(SessionClosedException.class);
        assertThatThrownBy(() -> submit.execute(world.user, session.id(), 1, Language.JAVA, "x"))
                .isInstanceOf(SessionClosedException.class);
        assertThatThrownBy(() -> run.execute(world.user, session.id(), 3, Language.JAVA, "x"))
                .isInstanceOf(ItemNotInSessionException.class)
                .hasMessage("Não existe desafio 3 nesta sessão.");
        assertThat(world.queue.published()).isEmpty();
    }

    @Test
    void aLessonHasNoChallenges() {
        Session lesson = world.openLesson();

        assertThatThrownBy(() -> submit.execute(world.user, lesson.id(), 1, Language.JAVA, "x"))
                .isInstanceOf(ItemNotInSessionException.class);
    }
}
