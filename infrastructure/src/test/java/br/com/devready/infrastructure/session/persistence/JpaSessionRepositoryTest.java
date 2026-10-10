package br.com.devready.infrastructure.session.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.session.exception.OpenSessionConflictException;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.Tag;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionQuestion;
import br.com.devready.domain.session.SessionStatus;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.catalog.persistence.JpaAreaRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaChallengeRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaQuestionRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaTagRepository;
import br.com.devready.infrastructure.evaluation.persistence.JpaSubmissionRepository;
import br.com.devready.infrastructure.identity.persistence.JpaUserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    PostgresTestcontainersConfiguration.class,
    JpaAreaRepository.class,
    JpaTagRepository.class,
    JpaQuestionRepository.class,
    JpaChallengeRepository.class,
    JpaUserRepository.class,
    JpaSubmissionRepository.class,
    JpaSessionRepository.class
})
class JpaSessionRepositoryTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    @Autowired private JpaAreaRepository areas;
    @Autowired private JpaTagRepository tags;
    @Autowired private JpaQuestionRepository questions;
    @Autowired private JpaChallengeRepository challenges;
    @Autowired private JpaUserRepository users;
    @Autowired private JpaSubmissionRepository submissions;
    @Autowired private JpaSessionRepository sessions;
    @Autowired private TestEntityManager entityManager;

    private record Fixture(Area area, Question first, Question second, Challenge challenge) {}

    private Fixture fixture() {
        String s = SessionTestData.suffix();
        Area area = areas.save(SessionTestData.area("zz-sess-" + s));
        tags.save(new Tag(UUID.randomUUID(), "zz-sess-" + s, "Sessão"));
        Question first =
                questions.save(
                        SessionTestData.question(
                                "zz-q1-" + s, area, Level.JUNIOR, true, "zz-sess-" + s));
        Question second =
                questions.save(
                        SessionTestData.question(
                                "zz-q2-" + s, area, Level.JUNIOR, true, "zz-sess-" + s));
        Challenge challenge =
                challenges.save(
                        SessionTestData.challenge(
                                "zz-c-" + s, area, Level.JUNIOR, true, "zz-sess-" + s));
        return new Fixture(area, first, second, challenge);
    }

    private UUID user(String name) {
        UUID id = UUID.randomUUID();
        users.save(User.register(id, name + "-" + id + "@example.com", name, "hash", T0));
        return id;
    }

    private Session simulation(Fixture f, UUID user, Instant at) {
        return Session.simulation(
                UUID.randomUUID(),
                user,
                f.area().id(),
                Level.JUNIOR,
                f.area().sessionConfig(),
                List.of(f.first().id(), f.second().id()),
                List.of(f.challenge().id()),
                at);
    }

    @Test
    void savesAndReadsBackTheSessionWithItsItems() {
        Fixture f = fixture();
        UUID ana = user("ana");
        Session created = sessions.create(simulation(f, ana, T0));
        entityManager.clear();

        Session read = sessions.findById(created.id()).orElseThrow();

        assertThat(read).isEqualTo(created);
        assertThat(read.deadlineAt()).isEqualTo(T0.plus(Duration.ofMinutes(45)));
        assertThat(read.questions())
                .extracting(SessionQuestion::questionId)
                .containsExactly(f.first().id(), f.second().id());
        assertThat(sessions.findOpen(ana)).contains(read);
        assertThat(sessions.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void theAnswerIsReplacedInTheSimulationAndOnlyOnceWithoutReplace() {
        Fixture f = fixture();
        Session session = sessions.create(simulation(f, user("bia"), T0));
        UUID wrong = f.first().options().get(0).id();
        UUID right = f.first().options().get(1).id();

        assertThat(sessions.answer(session.id(), 1, wrong, false, true)).isTrue();
        assertThat(sessions.answer(session.id(), 1, right, true, true)).isTrue();
        assertThat(sessions.answer(session.id(), 1, wrong, false, false)).isFalse();
        UUID secondRight = f.second().options().get(1).id();
        UUID secondWrong = f.second().options().get(0).id();
        assertThat(sessions.answer(session.id(), 2, secondWrong, false, false)).isTrue();
        assertThat(sessions.answer(session.id(), 2, secondRight, true, false)).isFalse();
        assertThat(sessions.answer(session.id(), 9, right, true, true)).isFalse();
        entityManager.clear();

        Session read = sessions.findById(session.id()).orElseThrow();
        assertThat(read.question(1).orElseThrow().optionId()).isEqualTo(right);
        assertThat(read.question(1).orElseThrow().hit()).isTrue();
        assertThat(read.question(2).orElseThrow().optionId()).isEqualTo(secondWrong);
        assertThat(read.question(2).orElseThrow().hit()).isFalse();
    }

    @Test
    void submissionsStopAtTheLimitAndNothingChangesAfterSubmitting() {
        Fixture f = fixture();
        Session session = sessions.create(simulation(f, user("caio"), T0));
        for (int i = 0; i < 3; i++) {
            assertThat(sessions.useSubmission(session.id(), 1, 3)).isTrue();
        }
        assertThat(sessions.useSubmission(session.id(), 1, 3)).isFalse();
        assertThat(sessions.useSubmission(session.id(), 1, 5)).isTrue();
        assertThat(sessions.useSubmission(session.id(), 1, 5)).isTrue();
        assertThat(sessions.useSubmission(session.id(), 1, 5)).isFalse();
        assertThat(sessions.useSubmission(session.id(), 2, 5)).isFalse();

        assertThat(sessions.submit(session.id())).isTrue();
        assertThat(sessions.submit(session.id())).isFalse();
        assertThat(sessions.answer(session.id(), 2, f.second().options().get(1).id(), true, true))
                .isFalse();
        entityManager.clear();

        Session read = sessions.findById(session.id()).orElseThrow();
        assertThat(read.status()).isEqualTo(SessionStatus.SUBMITTED);
        assertThat(read.challenges().get(0).submissionsUsed()).isEqualTo(5);
        assertThat(sessions.findSubmittedIds()).contains(session.id());
        assertThat(sessions.findOpen(read.userId())).isEmpty();
    }

    @Test
    void onlyTheSimulationsPastTheCutoffExpire() {
        Fixture f = fixture();
        Session late = sessions.create(simulation(f, user("davi"), T0));
        Session onTime = sessions.create(simulation(f, user("edu"), T0.plus(Duration.ofHours(1))));
        Session lesson =
                sessions.create(
                        Session.lesson(
                                UUID.randomUUID(),
                                user("fe"),
                                f.area().id(),
                                Level.JUNIOR,
                                "sql",
                                List.of(
                                        f.first().id(),
                                        f.second().id(),
                                        questions
                                                .save(
                                                        SessionTestData.question(
                                                                "zz-q3-" + SessionTestData.suffix(),
                                                                f.area(),
                                                                Level.JUNIOR,
                                                                true,
                                                                "x"))
                                                .id()),
                                T0));

        int expired = sessions.expireDue(T0.plus(Duration.ofMinutes(46)));
        entityManager.clear();

        assertThat(expired).isEqualTo(1);
        assertThat(sessions.findById(late.id()).orElseThrow().status())
                .isEqualTo(SessionStatus.SUBMITTED);
        assertThat(sessions.findById(onTime.id()).orElseThrow().status())
                .isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(sessions.findById(lesson.id()).orElseThrow().status())
                .isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(sessions.expireDue(T0.plus(Duration.ofMinutes(46)))).isZero();
    }

    @Test
    void theCorrectionIsSavedOnlyOnceAndKeepsTheBestSubmission() {
        Fixture f = fixture();
        UUID gil = user("gil");
        Session session = sessions.create(simulation(f, gil, T0));
        CodeSubmission best =
                submissions.save(
                        CodeSubmission.queueInSession(
                                UUID.randomUUID(),
                                gil,
                                f.challenge().id(),
                                Language.JAVA,
                                "x",
                                SubmissionKind.SUBMIT,
                                4,
                                session.id(),
                                T0));
        best = submissions.save(best.fail("fora", T0.plusSeconds(1)));
        sessions.submit(session.id());
        Session submitted = sessions.findById(session.id()).orElseThrow();
        Session completed = submitted.complete(Map.of(1, best), T0.plusSeconds(60));

        assertThat(sessions.complete(completed)).isTrue();
        assertThat(sessions.complete(completed)).isFalse();
        assertThat(sessions.fail(submitted.fail("x", T0.plusSeconds(61)))).isFalse();
        entityManager.clear();

        Session read = sessions.findById(session.id()).orElseThrow();
        assertThat(read).isEqualTo(completed);
        assertThat(read.challenges().get(0).bestSubmissionId()).isEqualTo(best.id());
        assertThat(sessions.findSubmittedIds()).doesNotContain(session.id());
    }

    @Test
    void aFailureKeepsTheReason() {
        Fixture f = fixture();
        Session session = sessions.create(simulation(f, user("hel"), T0));
        sessions.submit(session.id());
        Session failed =
                sessions.findById(session.id())
                        .orElseThrow()
                        .fail("Falha ao corrigir a sessão: x", T0);

        assertThat(sessions.fail(failed)).isTrue();
        entityManager.clear();

        assertThat(sessions.findById(session.id())).contains(failed);
    }

    @Test
    void theHistoryComesNewestFirst() {
        Fixture f = fixture();
        UUID ivo = user("ivo");
        Session old = sessions.create(simulation(f, ivo, T0));
        sessions.submit(old.id());
        Session recent = sessions.create(simulation(f, ivo, T0.plusSeconds(3_600)));
        entityManager.clear();

        assertThat(sessions.findByUser(ivo))
                .extracting(Session::id)
                .containsExactly(recent.id(), old.id());
    }

    @Test
    void aSubmittedSessionDoesNotBlockTheNextOne() {
        Fixture f = fixture();
        UUID kai = user("kai");
        Session first = sessions.create(simulation(f, kai, T0));
        sessions.submit(first.id());

        Session next = sessions.create(simulation(f, kai, T0.plusSeconds(1)));

        assertThat(sessions.findOpen(kai)).contains(next);
        assertThat(sessions.findSubmittedIds()).contains(first.id());
    }

    @Test
    void theSessionOfASubmissionSurvivesQueueStartAndFinish() {
        Fixture f = fixture();
        UUID lia = user("lia");
        Session session = sessions.create(simulation(f, lia, T0));
        CodeSubmission queued =
                CodeSubmission.queueInSession(
                        UUID.randomUUID(),
                        lia,
                        f.challenge().id(),
                        Language.JAVA,
                        "x",
                        SubmissionKind.SUBMIT,
                        4,
                        session.id(),
                        T0);

        submissions.save(queued);
        entityManager.clear();
        assertThat(submissions.findById(queued.id()).orElseThrow().sessionId())
                .isEqualTo(session.id());

        CodeSubmission running = submissions.save(queued.start());
        entityManager.clear();
        assertThat(submissions.findById(queued.id()).orElseThrow().sessionId())
                .isEqualTo(session.id());

        submissions.save(running.fail("fora", T0.plusSeconds(1)));
        entityManager.clear();
        assertThat(submissions.findById(queued.id()).orElseThrow().sessionId())
                .isEqualTo(session.id());
    }

    @Test
    void aSecondOpenSessionOfThePersonIsAConflict() {
        Fixture f = fixture();
        UUID jo = user("jo");
        sessions.create(simulation(f, jo, T0));

        assertThatThrownBy(() -> sessions.create(simulation(f, jo, T0.plusSeconds(1))))
                .isInstanceOf(OpenSessionConflictException.class);
    }
}
