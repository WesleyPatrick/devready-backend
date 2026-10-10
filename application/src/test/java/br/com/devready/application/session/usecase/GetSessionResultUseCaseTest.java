package br.com.devready.application.session.usecase;

import static br.com.devready.application.session.fake.SessionWorld.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.practice.service.ProblemBoard;
import br.com.devready.application.session.dto.ChallengeCorrectionView;
import br.com.devready.application.session.dto.CompetencyResultView;
import br.com.devready.application.session.dto.QuestionCorrectionView;
import br.com.devready.application.session.dto.SessionResultView;
import br.com.devready.application.session.exception.SessionNotFoundException;
import br.com.devready.application.session.exception.SessionResultNotReadyException;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.session.FeedbackVerdict;
import br.com.devready.domain.session.Session;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetSessionResultUseCaseTest {

    private final SessionWorld world = new SessionWorld();
    private final GetSessionResultUseCase result =
            new GetSessionResultUseCase(
                    world.sessions, world.pool, world.practice.submissions, world.practice.board);

    @Test
    void onlyACorrectedSessionHasAResult() {
        Session session = world.openSimulation();

        assertThatThrownBy(() -> result.execute(world.user, session.id()))
                .isInstanceOf(SessionResultNotReadyException.class)
                .hasMessage("O resultado sai quando a correção terminar.");
        assertThatThrownBy(() -> result.execute(UUID.randomUUID(), session.id()))
                .isInstanceOf(SessionNotFoundException.class);

        world.sessions.submit(session.id());
        assertThatThrownBy(() -> result.execute(world.user, session.id()))
                .isInstanceOf(SessionResultNotReadyException.class);
    }

    @Test
    void theResultHasTheCorrectionItemByItemAndTheFeedbackByCompetency() {
        world.practice.withGoal();
        Session session = world.openSimulation();
        for (String slug : new String[] {"q-sql-easy", "q-sql-hard", "q-java-sql"}) {
            world.sessions.answer(
                    session.id(),
                    world.position(session, slug),
                    world.option(slug, true),
                    true,
                    true);
        }
        world.sessions.answer(
                session.id(),
                world.position(session, "q-java"),
                world.option("q-java", false),
                false,
                true);
        CodeSubmission accepted =
                world.practice.submittedInSession(
                        world.user, "c-sql", SubmissionStatus.PASSED, 4, session.id(), T0);
        world.finishSession.execute(world.user, session.id());

        SessionResultView view = result.execute(world.user, session.id());

        QuestionCorrectionView javaQuestion =
                view.questions().get(world.position(session, "q-java") - 1);
        assertThat(javaQuestion.slug()).isEqualTo("q-java");
        assertThat(javaQuestion.correct()).isFalse();
        assertThat(javaQuestion.chosenOptionId()).isEqualTo(world.option("q-java", false));
        assertThat(javaQuestion.correctOptionId()).isEqualTo(world.option("q-java", true));
        assertThat(javaQuestion.explanation()).isEqualTo("Explicação de q-java");
        ChallengeCorrectionView sql =
                view.challenges().get(world.challengePosition(session, "c-sql") - 1);
        assertThat(sql.passedCount()).isEqualTo(4);
        assertThat(sql.totalCount()).isEqualTo(4);
        assertThat(sql.bestSubmissionId()).isEqualTo(accepted.id());
        ChallengeCorrectionView two =
                view.challenges().get(world.challengePosition(session, "c-two") - 1);
        assertThat(two.passedCount()).isZero();
        assertThat(two.totalCount()).isEqualTo(4);
        assertThat(two.bestSubmissionId()).isNull();
        CompetencyResultView sqlFeedback =
                view.competencies().stream()
                        .filter(c -> c.competency() != null && c.competency().slug().equals("sql"))
                        .findFirst()
                        .orElseThrow();
        assertThat(sqlFeedback.feedback().hits()).isEqualTo(4);
        assertThat(sqlFeedback.feedback().total()).isEqualTo(5);
        assertThat(sqlFeedback.feedback().verdict()).isEqualTo(FeedbackVerdict.STRONG);
        CompetencyResultView others = view.competencies().get(view.competencies().size() - 1);
        assertThat(others.competency()).isNull();
        assertThat(others.feedback().competencyName()).isEqualTo("Outros");
    }

    @Test
    void theTrackIsAssembledOnceForTheWholeResult() {
        world.practice.withGoal();
        CountingProfiles profiles = new CountingProfiles(world.practice.profiles);
        var practice = world.practice;
        TrackAssembler assembler =
                new TrackAssembler(
                        profiles,
                        practice.companies,
                        practice.stacks,
                        practice.competencies,
                        practice.tracks,
                        practice.areas,
                        practice.catalog,
                        practice.progress);
        GetSessionResultUseCase counted =
                new GetSessionResultUseCase(
                        world.sessions,
                        world.pool,
                        practice.submissions,
                        new ProblemBoard(
                                assembler,
                                practice.areas,
                                practice.catalog,
                                practice.competencies));
        Session session = world.openSimulation();
        world.finishSession.execute(world.user, session.id());

        SessionResultView view = counted.execute(world.user, session.id());

        assertThat(view.questions()).hasSize(8);
        assertThat(view.challenges()).hasSize(2);
        assertThat(profiles.reads).isEqualTo(1);
    }

    private static final class CountingProfiles implements CareerProfileRepository {
        private final CareerProfileRepository profiles;
        private int reads;

        CountingProfiles(CareerProfileRepository profiles) {
            this.profiles = profiles;
        }

        @Override
        public Optional<CareerProfile> findByUserId(UUID userId) {
            reads++;
            return profiles.findByUserId(userId);
        }

        @Override
        public CareerProfile save(CareerProfile profile) {
            return profiles.save(profile);
        }
    }
}
