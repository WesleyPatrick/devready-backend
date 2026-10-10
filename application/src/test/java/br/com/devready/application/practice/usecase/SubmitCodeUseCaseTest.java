package br.com.devready.application.practice.usecase;

import static br.com.devready.application.evaluation.fake.SubmissionFixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.evaluation.fake.RecordingSubmissionQueue;
import br.com.devready.application.practice.dto.SubmissionTicket;
import br.com.devready.application.practice.exception.ChallengeInExamException;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmitCodeUseCaseTest {

    private final PracticeWorld world = new PracticeWorld();
    private final RecordingSubmissionQueue queue = new RecordingSubmissionQueue();
    private final SubmitCodeUseCase submit =
            new SubmitCodeUseCase(
                    new SubmissionDesk(
                            world.challenges,
                            world.submissions,
                            queue,
                            new SequentialIdGenerator(),
                            new FixedClock(T0)),
                    world.examLocks,
                    world.levels);

    private CodeSubmission saved(SubmissionTicket ticket) {
        return world.submissions.findById(ticket.id()).orElseThrow();
    }

    @Test
    void queuesEveryCaseWorthTheXpOfTheDifficultyEvenWithoutAGoal() {
        Area backend = world.areas.findBySlug("backend").orElseThrow();
        world.challenges.save(
                CatalogFixtures.challenge("c-medio", "Médio", backend, Difficulty.MEDIUM, "sql"));
        world.challenges.save(
                CatalogFixtures.challenge("c-dificil", "Difícil", backend, Difficulty.HARD, "sql"));

        SubmissionTicket easy =
                submit.execute(world.user, "c-sql", Language.JAVA, "class Solution {}");
        SubmissionTicket medium =
                submit.execute(world.user, "c-medio", Language.JAVA, "class Solution {}");
        SubmissionTicket hard =
                submit.execute(world.user, "c-dificil", Language.JAVASCRIPT, "class Solution {}");

        assertThat(saved(easy).kind()).isEqualTo(SubmissionKind.SUBMIT);
        assertThat(saved(easy).totalCount()).isEqualTo(4);
        assertThat(saved(easy).xpReward()).isEqualTo(40);
        assertThat(saved(medium).xpReward()).isEqualTo(60);
        assertThat(saved(hard).xpReward()).isEqualTo(80);
        assertThat(queue.published()).containsExactly(easy.id(), medium.id(), hard.id());
    }

    @Test
    void aChallengeOfAnOpenSimulationIsNotSubmittedHere() {
        world.examLocks.lock(world.user, world.challenges.get("c-sql").id(), UUID.randomUUID());

        assertThatThrownBy(() -> submit.execute(world.user, "c-sql", Language.JAVA, "x"))
                .isInstanceOf(ChallengeInExamException.class);
        assertThat(queue.published()).isEmpty();
    }
}
