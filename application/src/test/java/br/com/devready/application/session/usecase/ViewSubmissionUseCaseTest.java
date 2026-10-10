package br.com.devready.application.session.usecase;

import static br.com.devready.application.session.fake.SessionWorld.T0;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.practice.dto.SubmissionView;
import br.com.devready.application.practice.usecase.GetSubmissionUseCase;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.session.Session;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ViewSubmissionUseCaseTest {

    private final SessionWorld world = new SessionWorld();
    private final ViewSubmissionUseCase view =
            new ViewSubmissionUseCase(
                    new GetSubmissionUseCase(
                            world.practice.submissions, world.practice.challenges, world.desk),
                    world.sessions);

    @Test
    void aSubmitOfAnOpenSessionIsSealedUntilTheCorrection() {
        Session session = world.openSimulation();
        CodeSubmission submit =
                world.practice.submittedInSession(
                        world.user, "c-sql", SubmissionStatus.FAILED, 2, session.id(), T0);

        SubmissionView open = view.execute(world.user, submit.id());
        assertThat(open.sealed()).isTrue();
        assertThat(open.cases()).isEmpty();
        assertThat(open.submission().passedCount()).isEqualTo(2);

        world.sessions.submit(session.id());
        assertThat(view.execute(world.user, submit.id()).sealed()).isTrue();

        world.finalizeSession.execute(session.id());
        assertThat(view.execute(world.user, submit.id()).sealed()).isFalse();
    }

    @Test
    void aRunOfTheSessionAndAPracticeSubmissionAreNeverSealed() {
        Session session = world.openSimulation();
        CodeSubmission run =
                world.practice.submissions.save(
                        CodeSubmission.queueInSession(
                                        UUID.randomUUID(),
                                        world.user,
                                        world.practice.challenges.get("c-sql").id(),
                                        Language.JAVA,
                                        "x",
                                        SubmissionKind.RUN,
                                        2,
                                        session.id(),
                                        T0)
                                .start()
                                .fail("fora", T0));
        CodeSubmission practice =
                world.practice.submitted(world.user, "c-sql", SubmissionStatus.FAILED, 0, T0);

        assertThat(view.execute(world.user, run.id()).sealed()).isFalse();
        assertThat(view.execute(world.user, practice.id()).sealed()).isFalse();
    }
}
