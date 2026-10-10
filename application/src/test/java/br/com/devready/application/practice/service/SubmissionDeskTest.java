package br.com.devready.application.practice.service;

import static br.com.devready.application.evaluation.fake.SubmissionFixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.evaluation.fake.RecordingSubmissionQueue;
import br.com.devready.application.practice.dto.SubmissionTicket;
import br.com.devready.application.practice.exception.LanguageNotOfferedException;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SubmissionDeskTest {

    private final PracticeWorld world = new PracticeWorld();
    private final RecordingSubmissionQueue queue = new RecordingSubmissionQueue();
    private final SubmissionDesk desk =
            new SubmissionDesk(
                    world.challenges,
                    world.submissions,
                    queue,
                    new SequentialIdGenerator(),
                    new FixedClock(T0));
    private final UUID session = UUID.randomUUID();

    @Test
    void aSessionSubmissionGoesToTheQueueWithTheSessionAndWithoutXp() {
        Challenge challenge = world.challenges.get("c-sql");

        SubmissionTicket ticket =
                desk.queueInSession(
                        world.user,
                        challenge,
                        Language.PYTHON,
                        "class Solution: pass",
                        SubmissionKind.SUBMIT,
                        session);

        CodeSubmission saved = world.submissions.findById(ticket.id()).orElseThrow();
        assertThat(ticket.status()).isEqualTo(SubmissionStatus.QUEUED);
        assertThat(saved.sessionId()).isEqualTo(session);
        assertThat(saved.xpReward()).isZero();
        assertThat(saved.totalCount()).isEqualTo(4);
        assertThat(saved.createdAt()).isEqualTo(T0);
        assertThat(queue.published()).containsExactly(ticket.id());
    }

    @Test
    void aSessionRunHasOnlyTheVisibleCases() {
        SubmissionTicket ticket =
                desk.queueInSession(
                        world.user,
                        world.challenges.get("c-sql"),
                        Language.JAVA,
                        "class Solution {}",
                        SubmissionKind.RUN,
                        session);

        assertThat(world.submissions.findById(ticket.id()).orElseThrow().totalCount()).isEqualTo(2);
    }

    @Test
    void anInactiveChallengeOfTheSessionStillTakesSubmissions() {
        Challenge inactive =
                world.challenges.save(
                        CatalogFixtures.challenge(
                                "c-old",
                                "Antigo",
                                world.areas.findBySlug("backend").orElseThrow(),
                                Difficulty.EASY,
                                false,
                                List.of(Language.JAVA),
                                "sql"));

        desk.queueInSession(
                world.user, inactive, Language.JAVA, "x", SubmissionKind.SUBMIT, session);

        assertThat(queue.published()).hasSize(1);
    }

    @Test
    void aLanguageTheChallengeDoesNotOfferSavesNothing() {
        Challenge javaOnly =
                world.challenges.save(
                        CatalogFixtures.challenge(
                                "c-java",
                                "Só Java",
                                world.areas.findBySlug("backend").orElseThrow(),
                                Difficulty.EASY,
                                true,
                                List.of(Language.JAVA),
                                "sql"));

        assertThatThrownBy(
                        () ->
                                desk.queueInSession(
                                        world.user,
                                        javaOnly,
                                        Language.PYTHON,
                                        "x",
                                        SubmissionKind.SUBMIT,
                                        session))
                .isInstanceOf(LanguageNotOfferedException.class);
        assertThat(world.submissions.all()).isEmpty();
        assertThat(queue.published()).isEmpty();
    }
}
