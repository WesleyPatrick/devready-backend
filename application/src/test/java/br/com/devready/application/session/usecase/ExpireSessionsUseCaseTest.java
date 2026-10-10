package br.com.devready.application.session.usecase;

import static br.com.devready.application.session.fake.SessionWorld.T0;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.session.dto.ExpiryRound;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionStatus;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExpireSessionsUseCaseTest {

    private final SessionWorld world = new SessionWorld();
    private final ExpireSessionsUseCase expire =
            new ExpireSessionsUseCase(world.sessions, world.finalizeSession, world.clock);

    @Test
    void aSimulationPastTheDeadlineAndTheGraceIsSubmittedAndCorrected() {
        Session session = world.openSimulation();
        world.clock.advanceTo(session.deadlineAt().plusSeconds(30));
        assertThat(expire.execute()).isEqualTo(new ExpiryRound(0, List.of()));
        assertThat(world.sessions.get(session.id()).status()).isEqualTo(SessionStatus.IN_PROGRESS);

        world.clock.advanceTo(session.deadlineAt().plusSeconds(31));
        ExpiryRound round = expire.execute();

        assertThat(round.expired()).isEqualTo(1);
        assertThat(round.closed()).extracting(Session::id).containsExactly(session.id());
        assertThat(world.sessions.get(session.id()).status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(expire.execute()).isEqualTo(new ExpiryRound(0, List.of()));
    }

    @Test
    void aLessonNeverExpires() {
        Session lesson = world.openLesson();
        world.clock.advanceTo(T0.plus(Duration.ofDays(3)));

        assertThat(expire.execute().expired()).isZero();
        assertThat(world.sessions.get(lesson.id()).status()).isEqualTo(SessionStatus.IN_PROGRESS);
    }

    @Test
    void aSubmittedSessionWaitingForAStuckSubmissionIsCorrectedOnALaterRound() {
        Session session = world.openSimulation();
        world.practice.submissions.save(
                CodeSubmission.queueInSession(
                        UUID.randomUUID(),
                        world.user,
                        world.practice.challenges.get("c-sql").id(),
                        Language.JAVA,
                        "x",
                        SubmissionKind.SUBMIT,
                        4,
                        session.id(),
                        T0));
        world.finishSession.execute(world.user, session.id());
        assertThat(expire.execute().closed()).isEmpty();

        world.clock.advanceTo(T0.plus(Duration.ofMinutes(3)));
        ExpiryRound round = expire.execute();

        assertThat(round.expired()).isZero();
        assertThat(round.closed())
                .extracting(Session::status)
                .containsExactly(SessionStatus.COMPLETED);
    }
}
