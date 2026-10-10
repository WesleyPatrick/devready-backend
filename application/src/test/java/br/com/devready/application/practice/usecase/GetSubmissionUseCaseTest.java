package br.com.devready.application.practice.usecase;

import static br.com.devready.application.evaluation.fake.SubmissionFixtures.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.evaluation.fake.RecordingSubmissionQueue;
import br.com.devready.application.evaluation.fake.SubmissionFixtures;
import br.com.devready.application.practice.dto.CaseView;
import br.com.devready.application.practice.dto.SubmissionView;
import br.com.devready.application.practice.exception.SubmissionNotFoundException;
import br.com.devready.application.practice.fake.PracticeWorld;
import br.com.devready.application.practice.service.SubmissionDesk;
import br.com.devready.application.shared.FixedClock;
import br.com.devready.application.shared.SequentialIdGenerator;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.evaluation.CaseResult;
import br.com.devready.domain.evaluation.CaseStatus;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.evaluation.Verdict;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetSubmissionUseCaseTest {

    private final PracticeWorld world = new PracticeWorld();
    private final FixedClock clock = new FixedClock(T0.plusSeconds(10));
    private final GetSubmissionUseCase getSubmission =
            new GetSubmissionUseCase(
                    world.submissions,
                    world.challenges,
                    new SubmissionDesk(
                            world.challenges,
                            world.submissions,
                            new RecordingSubmissionQueue(),
                            new SequentialIdGenerator(),
                            clock));

    private Challenge challenge() {
        return world.challenges.get("c-sql");
    }

    @Test
    void showsTheVisibleCasesWholeAndOnlyTheStatusOfTheHiddenOnes() {
        Verdict verdict =
                new Verdict(
                        SubmissionStatus.FAILED,
                        List.of(
                                new CaseResult(0, true, CaseStatus.PASSED, "1", null, 2),
                                new CaseResult(1, true, CaseStatus.FAILED, "99", null, 3),
                                new CaseResult(2, false, CaseStatus.PASSED, "3", null, 1),
                                new CaseResult(3, false, CaseStatus.ERROR, null, "IndexError", 1)),
                        null,
                        null);
        CodeSubmission done =
                world.submissions.save(
                        SubmissionFixtures.queued(
                                        world.user, challenge(), SubmissionKind.SUBMIT, 40, T0)
                                .start()
                                .finish(verdict, false, T0.plusSeconds(3)));

        SubmissionView view = getSubmission.execute(world.user, done.id());

        assertThat(view.challengeSlug()).isEqualTo("c-sql");
        assertThat(view.submission().status()).isEqualTo(SubmissionStatus.FAILED);
        assertThat(view.submission().passedCount()).isEqualTo(2);
        CaseView second = view.cases().get(1);
        assertThat(second.visible()).isTrue();
        assertThat(second.status()).isEqualTo(CaseStatus.FAILED);
        assertThat(second.argsJson()).isEqualTo("[2]");
        assertThat(second.expectedJson()).isEqualTo("2");
        assertThat(second.actualJson()).isEqualTo("99");
        assertThat(second.timeMs()).isEqualTo(3);
        CaseView hidden = view.cases().get(3);
        assertThat(hidden.visible()).isFalse();
        assertThat(hidden.status()).isEqualTo(CaseStatus.ERROR);
        assertThat(hidden.argsJson()).isNull();
        assertThat(hidden.expectedJson()).isNull();
        assertThat(hidden.actualJson()).isNull();
        assertThat(hidden.error()).isNull();
        assertThat(hidden.timeMs()).isEqualTo(1);
    }

    @Test
    void aCaseRecordedAsVisibleThatIsHiddenNowStaysHidden() {
        Verdict verdict =
                new Verdict(
                        SubmissionStatus.FAILED,
                        List.of(
                                new CaseResult(0, true, CaseStatus.PASSED, "1", null, 2),
                                new CaseResult(2, true, CaseStatus.FAILED, "99", "boom", 3)),
                        null,
                        null);
        CodeSubmission done =
                world.submissions.save(
                        SubmissionFixtures.queued(
                                        world.user, challenge(), SubmissionKind.SUBMIT, 40, T0)
                                .start()
                                .finish(verdict, false, T0.plusSeconds(3)));

        CaseView view = getSubmission.execute(world.user, done.id()).cases().get(1);

        assertThat(view.status()).isEqualTo(CaseStatus.FAILED);
        assertThat(view.argsJson()).isNull();
        assertThat(view.expectedJson()).isNull();
        assertThat(view.actualJson()).isNull();
        assertThat(view.error()).isNull();
    }

    @Test
    void aSubmissionOfAnotherPersonOrUnknownIsNotFound() {
        CodeSubmission other =
                world.submissions.save(
                        SubmissionFixtures.queued(
                                UUID.randomUUID(), challenge(), SubmissionKind.SUBMIT, 40, T0));

        assertThatThrownBy(() -> getSubmission.execute(world.user, other.id()))
                .isInstanceOf(SubmissionNotFoundException.class)
                .hasMessage("Envio não encontrado.");
        assertThatThrownBy(() -> getSubmission.execute(world.user, UUID.randomUUID()))
                .isInstanceOf(SubmissionNotFoundException.class);
    }

    @Test
    void aSubmissionStuckForMoreThanTwoMinutesBecomesAnErrorAndIsSaved() {
        CodeSubmission queued =
                world.submissions.save(
                        SubmissionFixtures.queued(
                                world.user, challenge(), SubmissionKind.SUBMIT, 40, T0));

        clock.advanceTo(T0.plus(Duration.ofMinutes(2)));
        assertThat(getSubmission.execute(world.user, queued.id()).submission().status())
                .isEqualTo(SubmissionStatus.QUEUED);

        clock.advanceTo(T0.plus(Duration.ofMinutes(2)).plusSeconds(1));
        SubmissionView view = getSubmission.execute(world.user, queued.id());

        assertThat(view.submission().status()).isEqualTo(SubmissionStatus.ERROR);
        assertThat(view.submission().errorMessage())
                .isEqualTo("O envio não terminou a tempo. Tente de novo.");
        assertThat(world.submissions.findById(queued.id()).orElseThrow().status())
                .isEqualTo(SubmissionStatus.ERROR);
    }

    @Test
    void theViewOfAPracticeSubmissionIsNotSealedAndTheSealedCopyHidesTheCases() {
        CodeSubmission submission =
                world.submissions.save(
                        SubmissionFixtures.finishedInSession(
                                world.user,
                                world.challenges.get("c-sql").id(),
                                SubmissionStatus.FAILED,
                                2,
                                UUID.randomUUID(),
                                T0));

        SubmissionView view = getSubmission.execute(world.user, submission.id());

        assertThat(view.sealed()).isFalse();
        assertThat(view.sealedView().sealed()).isTrue();
        assertThat(view.sealedView().cases()).isEmpty();
        assertThat(view.sealedView().submission()).isEqualTo(view.submission());
    }

    @Test
    void theSealedCopyOfASubmissionWithResultsHidesEveryCase() {
        Verdict verdict =
                new Verdict(
                        SubmissionStatus.FAILED,
                        List.of(
                                new CaseResult(0, true, CaseStatus.PASSED, "1", null, 2),
                                new CaseResult(2, false, CaseStatus.FAILED, "9", "boom", 3)),
                        "saída do compilador",
                        null);
        CodeSubmission done =
                world.submissions.save(
                        SubmissionFixtures.queued(
                                        world.user, challenge(), SubmissionKind.SUBMIT, 40, T0)
                                .start()
                                .finish(verdict, false, T0.plusSeconds(3)));

        SubmissionView view = getSubmission.execute(world.user, done.id());

        assertThat(view.cases()).isNotEmpty();
        assertThat(view.sealed()).isFalse();
        assertThat(view.sealedView().cases()).isEmpty();
        assertThat(view.sealedView().sealed()).isTrue();
    }
}
