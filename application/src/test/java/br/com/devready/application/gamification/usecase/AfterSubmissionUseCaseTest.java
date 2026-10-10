package br.com.devready.application.gamification.usecase;

import static br.com.devready.application.session.fake.SessionWorld.T0;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.gamification.fake.InMemoryAchievementRepository;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.UnlockedAchievement;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AfterSubmissionUseCaseTest {

    private final SessionWorld world = new SessionWorld();
    private final InMemoryAchievementRepository achievements = new InMemoryAchievementRepository();
    private final AwardAchievementsUseCase award =
            new AwardAchievementsUseCase(
                    world.practice.answers,
                    world.practice.attempts,
                    world.practice.simulations,
                    world.practice.assembler,
                    achievements,
                    world.clock);
    private final AfterSubmissionUseCase after =
            new AfterSubmissionUseCase(world.practice.submissions, world.finalizeSession, award);

    @Test
    void theLastSubmissionOfASubmittedSessionCorrectsItAndTheAchievementsKeepTheSession() {
        Session session = world.openSimulation();
        CodeSubmission pending =
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
        world.practice.submissions.save(pending.fail("terminou", T0.plusSeconds(5)));
        world.practice.simulations.complete(world.user, T0.plusSeconds(5), 10);

        after.execute(pending.id());

        assertThat(world.sessions.get(session.id()).status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(achievements.all())
                .extracting(UnlockedAchievement::achievement, UnlockedAchievement::sessionId)
                .contains(
                        org.assertj.core.groups.Tuple.tuple(
                                Achievement.FIRST_SIMULATION, session.id()));
    }

    @Test
    void aPracticeSubmissionOnlyChecksTheAchievements() {
        world.practice.answered(world.user, "q-java", false, 0, T0);
        CodeSubmission practice =
                world.practice.submitted(world.user, "c-sql", SubmissionStatus.FAILED, 0, T0);

        after.execute(practice.id());

        assertThat(achievements.all())
                .extracting(UnlockedAchievement::achievement)
                .containsExactly(Achievement.FIRST_QUESTION);
    }

    @Test
    void anUnfinishedOrUnknownSubmissionDoesNothing() {
        world.practice.answered(world.user, "q-java", false, 0, T0);
        Session session = world.openSimulation();
        CodeSubmission pending =
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

        after.execute(pending.id());
        after.execute(UUID.randomUUID());

        assertThat(achievements.all()).isEmpty();
    }
}
