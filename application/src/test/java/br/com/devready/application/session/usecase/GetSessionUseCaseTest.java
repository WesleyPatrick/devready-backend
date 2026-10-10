package br.com.devready.application.session.usecase;

import static br.com.devready.application.session.fake.SessionWorld.T0;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.evaluation.fake.FixedLanguageRuntimes;
import br.com.devready.application.practice.dto.ChallengeLanguageView;
import br.com.devready.application.session.dto.SessionChallengeView;
import br.com.devready.application.session.dto.SessionQuestionView;
import br.com.devready.application.session.dto.SessionView;
import br.com.devready.application.session.exception.SessionNotFoundException;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.application.session.service.SessionViews;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.session.Session;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class GetSessionUseCaseTest {

    private final SessionWorld world = new SessionWorld();
    private final SessionViews views =
            new SessionViews(
                    world.pool,
                    world.practice.submissions,
                    world.practice.competencies,
                    new FixedLanguageRuntimes());
    private final GetSessionUseCase get = new GetSessionUseCase(world.sessions, views);
    private final GetCurrentSessionUseCase current =
            new GetCurrentSessionUseCase(world.sessions, views);

    @Test
    void theSimulationComesWithTheAreaTheItemsAndNoAnswerKey() {
        Session session = world.openSimulation();
        int position = world.position(session, "q-java");
        world.sessions.answer(session.id(), position, world.option("q-java", true), true, true);

        SessionView view = get.execute(world.user, session.id());

        assertThat(view.area().slug()).isEqualTo("backend");
        assertThat(view.competency()).isNull();
        assertThat(view.questions()).hasSize(8);
        SessionQuestionView answered = view.questions().get(position - 1);
        assertThat(answered.chosenOptionId()).isEqualTo(world.option("q-java", true));
        assertThat(answered.revealed()).isFalse();
        assertThat(answered.correct()).isNull();
        SessionChallengeView challenge = view.challenges().get(0);
        assertThat(challenge.position()).isEqualTo(1);
        assertThat(challenge.languages())
                .extracting(ChallengeLanguageView::runtime)
                .containsExactly("Java 13", "Python 3.8", "Node 12");
        assertThat(challenge.visibleCases()).extracting(TestCase::position).containsExactly(0, 1);
        assertThat(challenge.submissionsLeft()).isEqualTo(5);
        assertThat(challenge.bestSubmissionId()).isNull();
        assertThat(challenge.lastSubmissionId()).isNull();
    }

    @Test
    void theBestAndTheLastSubmissionComeFromTheSubmissionsOfTheSession() {
        Session session = world.openSimulation();
        int position = world.challengePosition(session, "c-sql");
        CodeSubmission best =
                world.practice.submittedInSession(
                        world.user, "c-sql", SubmissionStatus.FAILED, 3, session.id(), T0);
        CodeSubmission last =
                world.practice.submittedInSession(
                        world.user,
                        "c-sql",
                        SubmissionStatus.FAILED,
                        1,
                        session.id(),
                        T0.plusSeconds(30));
        world.sessions.useSubmission(session.id(), position, 5);
        world.sessions.useSubmission(session.id(), position, 5);

        SessionChallengeView view =
                get.execute(world.user, session.id()).challenges().get(position - 1);

        assertThat(view.bestSubmissionId()).isEqualTo(best.id());
        assertThat(view.lastSubmissionId()).isEqualTo(last.id());
        assertThat(view.submissionsUsed()).isEqualTo(2);
        assertThat(view.submissionsLeft()).isEqualTo(3);
    }

    @Test
    void theLessonComesWithTheCompetencyAndRevealsWhatWasAnswered() {
        Session lesson = world.openLesson();
        world.sessions.answer(lesson.id(), 1, world.option("q-sql-easy", false), false, false);

        SessionView view = get.execute(world.user, lesson.id());

        assertThat(view.competency().slug()).isEqualTo("sql");
        assertThat(view.challenges()).isEmpty();
        assertThat(view.questions().get(0).revealed()).isTrue();
        assertThat(view.questions().get(0).correct()).isFalse();
        assertThat(view.questions().get(1).revealed()).isFalse();
    }

    @Test
    void theCorrectedSimulationRevealsEverythingAndAnUnansweredQuestionIsWrong() {
        Session session = world.openSimulation();
        world.finishSession.execute(world.user, session.id());

        SessionView view = get.execute(world.user, session.id());

        assertThat(view.questions()).allMatch(SessionQuestionView::revealed);
        assertThat(view.questions()).allMatch(q -> Boolean.FALSE.equals(q.correct()));
    }

    @Test
    void theCurrentSessionIsTheOpenOne() {
        assertThatThrownBy(() -> current.execute(world.user))
                .isInstanceOf(SessionNotFoundException.class);
        Session session = world.openSimulation();

        assertThat(current.execute(world.user).session().id()).isEqualTo(session.id());
        assertThatThrownBy(() -> get.execute(UUID.randomUUID(), session.id()))
                .isInstanceOf(SessionNotFoundException.class);
    }
}
