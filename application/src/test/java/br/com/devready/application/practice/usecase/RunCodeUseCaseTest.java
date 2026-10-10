package br.com.devready.application.practice.usecase;

import static br.com.devready.application.evaluation.fake.SubmissionFixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.evaluation.fake.RecordingSubmissionQueue;
import br.com.devready.application.practice.dto.SubmissionTicket;
import br.com.devready.application.practice.exception.ChallengeInExamException;
import br.com.devready.application.practice.exception.ChallengeNotFoundException;
import br.com.devready.application.practice.exception.LanguageNotOfferedException;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RunCodeUseCaseTest {

    private final PracticeWorld world = new PracticeWorld();
    private final RecordingSubmissionQueue queue = new RecordingSubmissionQueue();
    private final RunCodeUseCase run =
            new RunCodeUseCase(
                    new SubmissionDesk(
                            world.challenges,
                            world.submissions,
                            queue,
                            new SequentialIdGenerator(),
                            new FixedClock(T0)),
                    world.examLocks,
                    world.levels);

    @Test
    void queuesARunOfTheVisibleCasesWorthNoXpAndPublishesIt() {
        SubmissionTicket ticket =
                run.execute(world.user, "c-sql", Language.PYTHON, "class Solution:\n    pass\n");

        CodeSubmission saved = world.submissions.findById(ticket.id()).orElseThrow();
        assertThat(ticket.status()).isEqualTo(SubmissionStatus.QUEUED);
        assertThat(saved.kind()).isEqualTo(SubmissionKind.RUN);
        assertThat(saved.language()).isEqualTo(Language.PYTHON);
        assertThat(saved.sourceCode()).isEqualTo("class Solution:\n    pass\n");
        assertThat(saved.totalCount()).isEqualTo(2);
        assertThat(saved.xpReward()).isZero();
        assertThat(saved.createdAt()).isEqualTo(T0);
        assertThat(saved.challengeId()).isEqualTo(world.challenges.get("c-sql").id());
        assertThat(queue.published()).containsExactly(ticket.id());
    }

    @Test
    void aLanguageTheChallengeDoesNotOfferIsRefusedAndNothingIsQueued() {
        world.challenges.save(
                CatalogFixtures.challenge(
                        "c-java",
                        "Só Java",
                        world.areas.findBySlug("backend").orElseThrow(),
                        Difficulty.EASY,
                        true,
                        List.of(Language.JAVA),
                        "sql"));

        assertThatThrownBy(() -> run.execute(world.user, "c-java", Language.PYTHON, "x"))
                .isInstanceOf(LanguageNotOfferedException.class)
                .hasMessage("Este desafio não tem a linguagem Python.");
        assertThat(world.submissions.all()).isEmpty();
        assertThat(queue.published()).isEmpty();
    }

    @Test
    void anUnknownChallengeIsNotFound() {
        assertThatThrownBy(() -> run.execute(world.user, "nao-existe", Language.JAVA, "x"))
                .isInstanceOf(ChallengeNotFoundException.class);
        assertThat(queue.published()).isEmpty();
    }

    @Test
    void aChallengeOfAnOpenSimulationDoesNotRunHere() {
        world.examLocks.lock(world.user, world.challenges.get("c-sql").id(), UUID.randomUUID());

        assertThatThrownBy(() -> run.execute(world.user, "c-sql", Language.JAVA, "x"))
                .isInstanceOf(ChallengeInExamException.class);
        assertThat(queue.published()).isEmpty();
        assertThat(world.submissions.all()).isEmpty();
    }
}
