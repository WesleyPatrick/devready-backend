package br.com.devready.infrastructure.session.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.Tag;
import br.com.devready.domain.evaluation.CaseResult;
import br.com.devready.domain.evaluation.CaseStatus;
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.evaluation.Verdict;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.practice.CompletedSimulation;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionStatus;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.catalog.persistence.JpaAreaRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaChallengeRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaQuestionRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaSessionPool;
import br.com.devready.infrastructure.catalog.persistence.JpaTagRepository;
import br.com.devready.infrastructure.evaluation.persistence.JpaSubmissionRepository;
import br.com.devready.infrastructure.identity.persistence.JpaUserRepository;
import br.com.devready.infrastructure.practice.persistence.JpaAnswerRepository;
import br.com.devready.infrastructure.practice.persistence.JpaChallengeAttemptReader;
import br.com.devready.infrastructure.practice.persistence.JpaExamLockReader;
import br.com.devready.infrastructure.practice.persistence.JpaQuestionProgressReader;
import br.com.devready.infrastructure.practice.persistence.JpaSimulationRecordReader;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
    JpaAnswerRepository.class,
    JpaSubmissionRepository.class,
    JpaChallengeAttemptReader.class,
    JpaQuestionProgressReader.class,
    JpaSessionRepository.class,
    JpaSessionPool.class,
    JpaSimulationRecordReader.class,
    JpaExamLockReader.class
})
class JpaSessionReadersTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    @Autowired private JpaAreaRepository areas;
    @Autowired private JpaTagRepository tags;
    @Autowired private JpaQuestionRepository questions;
    @Autowired private JpaChallengeRepository challenges;
    @Autowired private JpaUserRepository users;
    @Autowired private JpaSubmissionRepository submissions;
    @Autowired private JpaChallengeAttemptReader attempts;
    @Autowired private JpaQuestionProgressReader progress;
    @Autowired private JpaSessionRepository sessions;
    @Autowired private JpaSessionPool pool;
    @Autowired private JpaSimulationRecordReader simulations;
    @Autowired private JpaExamLockReader examLocks;
    @Autowired private TestEntityManager entityManager;

    private record Fixture(
            Area area, String tag, Question first, Question second, Challenge challenge) {}

    private Fixture fixture() {
        String s = SessionTestData.suffix();
        String tag = "zz-read-" + s;
        Area area = areas.save(SessionTestData.area(tag));
        tags.save(new Tag(UUID.randomUUID(), tag, "Leitura"));
        tags.save(new Tag(UUID.randomUUID(), tag + "-x", "Outra"));
        Question first =
                questions.save(
                        SessionTestData.question("zz-r1-" + s, area, Level.JUNIOR, true, tag));
        Question second =
                questions.save(
                        SessionTestData.question("zz-r2-" + s, area, Level.JUNIOR, true, tag));
        questions.save(SessionTestData.question("zz-r3-" + s, area, Level.PLENO, true, tag));
        questions.save(SessionTestData.question("zz-r4-" + s, area, Level.JUNIOR, false, tag));
        questions.save(
                SessionTestData.question("zz-r5-" + s, area, Level.SENIOR, true, tag + "-x"));
        Challenge challenge =
                challenges.save(
                        SessionTestData.challenge("zz-rc-" + s, area, Level.JUNIOR, true, tag));
        return new Fixture(area, tag, first, second, challenge);
    }

    private UUID user(String name) {
        UUID id = UUID.randomUUID();
        users.save(User.register(id, name + "-" + id + "@example.com", name, "hash", T0));
        return id;
    }

    private Session open(Fixture f, UUID user) {
        return sessions.create(
                Session.simulation(
                        UUID.randomUUID(),
                        user,
                        f.area().id(),
                        Level.JUNIOR,
                        f.area().sessionConfig(),
                        List.of(f.first().id(), f.second().id()),
                        List.of(f.challenge().id()),
                        T0));
    }

    private CodeSubmission finished(Fixture f, UUID user, UUID session, int passed) {
        CodeSubmission queued =
                submissions.save(
                        CodeSubmission.queueInSession(
                                UUID.randomUUID(),
                                user,
                                f.challenge().id(),
                                Language.JAVA,
                                "x",
                                SubmissionKind.SUBMIT,
                                2,
                                session,
                                T0));
        List<CaseResult> results =
                List.of(
                        new CaseResult(0, true, CaseStatus.PASSED, "1", null, 1),
                        new CaseResult(
                                1,
                                true,
                                passed == 2 ? CaseStatus.PASSED : CaseStatus.FAILED,
                                "0",
                                null,
                                1));
        return submissions.save(
                submissions
                        .save(queued.start())
                        .finish(
                                new Verdict(
                                        passed == 2
                                                ? SubmissionStatus.PASSED
                                                : SubmissionStatus.FAILED,
                                        results,
                                        null,
                                        null),
                                false,
                                T0.plusSeconds(5)));
    }

    @Test
    void thePoolReadsActiveItemsByAreaLevelTagsAndId() {
        Fixture f = fixture();

        assertThat(pool.area(f.area().id())).map(Area::slug).contains(f.area().slug());
        assertThat(pool.activeQuestions(f.area().id(), Level.JUNIOR))
                .extracting(Question::id)
                .containsExactlyInAnyOrder(f.first().id(), f.second().id());
        assertThat(pool.activeChallenges(f.area().id(), Level.JUNIOR))
                .extracting(Challenge::id)
                .containsExactly(f.challenge().id());
        assertThat(pool.activeChallenges(f.area().id(), Level.PLENO)).isEmpty();
        assertThat(
                        pool.activeQuestionsTagged(
                                f.area().id(), Set.of(f.tag()), Level.upTo(Level.SENIOR)))
                .extracting(Question::slug)
                .hasSize(3)
                .allMatch(slug -> !slug.startsWith("zz-r4") && !slug.startsWith("zz-r5"));
        assertThat(
                        pool.activeQuestionsTagged(
                                f.area().id(), Set.of(f.tag()), Level.upTo(Level.JUNIOR)))
                .extracting(Question::id)
                .containsExactlyInAnyOrder(f.first().id(), f.second().id());
        assertThat(pool.activeQuestionsTagged(f.area().id(), Set.of(), Level.upTo(Level.SENIOR)))
                .isEmpty();
        assertThat(pool.activeQuestionsTagged(f.area().id(), Set.of(f.tag()), Set.of())).isEmpty();
        assertThat(pool.questions(List.of(f.first().id())).get(0).options()).hasSize(2);
        assertThat(pool.challenges(List.of(f.challenge().id())).get(0).testCases()).hasSize(4);
        assertThat(pool.questions(List.of())).isEmpty();
    }

    @Test
    void thePoolReadsTheCollectionsInBatchesAndNotOneItemAtATime() {
        String s = SessionTestData.suffix();
        String tag = "zz-batch-" + s;
        Area area = areas.save(SessionTestData.area(tag));
        tags.save(new Tag(UUID.randomUUID(), tag, "Lote"));
        for (int i = 0; i < 6; i++) {
            questions.save(
                    SessionTestData.question("zz-bq" + i + "-" + s, area, Level.JUNIOR, true, tag));
        }
        for (int i = 0; i < 3; i++) {
            challenges.save(
                    SessionTestData.challenge(
                            "zz-bc" + i + "-" + s, area, Level.JUNIOR, true, tag));
        }
        entityManager.flush();
        entityManager.clear();
        Statistics statistics =
                entityManager
                        .getEntityManager()
                        .getEntityManagerFactory()
                        .unwrap(SessionFactory.class)
                        .getStatistics();
        statistics.setStatisticsEnabled(true);
        try {
            statistics.clear();

            assertThat(pool.activeQuestions(area.id(), Level.JUNIOR)).hasSize(6);
            assertThat(pool.activeChallenges(area.id(), Level.JUNIOR)).hasSize(3);

            assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(7);
        } finally {
            statistics.setStatisticsEnabled(false);
        }
    }

    @Test
    void hitsAndTheBonusCountOnlyAfterTheCorrection() {
        Fixture f = fixture();
        UUID ana = user("ana");
        Session session = open(f, ana);
        sessions.answer(session.id(), 1, f.first().options().get(1).id(), true, true);
        sessions.answer(session.id(), 2, f.second().options().get(0).id(), false, true);
        CodeSubmission accepted = finished(f, ana, session.id(), 2);
        entityManager.flush();

        assertThat(simulations.questionsSolvedBy(ana)).isEmpty();
        assertThat(simulations.challengesSolvedBy(ana)).isEmpty();
        assertThat(simulations.completedBy(ana)).isEmpty();
        assertThat(progress.answeredBy(ana)).isEmpty();

        sessions.submit(session.id());
        Session submitted = sessions.findById(session.id()).orElseThrow();
        sessions.complete(submitted.complete(Map.of(1, accepted), T0.plusSeconds(60)));
        entityManager.clear();

        assertThat(simulations.questionsSolvedBy(ana)).containsExactly(f.first().id());
        assertThat(simulations.challengesSolvedBy(ana)).containsExactly(f.challenge().id());
        assertThat(simulations.completedBy(ana))
                .containsExactly(new CompletedSimulation(T0.plusSeconds(60), 75));
        assertThat(progress.answeredBy(ana))
                .containsOnly(
                        entry(f.first().slug(), QuestionProgress.SOLVED),
                        entry(f.challenge().slug(), QuestionProgress.SOLVED));
    }

    @Test
    void theChallengesOfAnOpenSimulationStayLockedUntilTheCorrection() {
        Fixture f = fixture();
        UUID caio = user("caio");
        Session session = open(f, caio);

        assertThat(examLocks.lockedChallenges(caio))
                .containsExactly(Map.entry(f.challenge().id(), session.id()));
        assertThat(examLocks.lockedChallenges(UUID.randomUUID())).isEmpty();
        sessions.submit(session.id());
        assertThat(examLocks.lockedChallenges(caio)).containsKey(f.challenge().id());

        Session submitted = sessions.findById(session.id()).orElseThrow();
        sessions.complete(submitted.complete(Map.of(), T0.plusSeconds(60)));
        entityManager.clear();

        assertThat(examLocks.lockedChallenges(caio)).isEmpty();
    }

    @Test
    void theQuestionsOfAnOpenSimulationStayLockedUntilTheCorrection() {
        Fixture f = fixture();
        UUID dani = user("dani");
        Session session = open(f, dani);

        assertThat(examLocks.lockedQuestions(dani))
                .containsOnly(
                        Map.entry(f.first().id(), session.id()),
                        Map.entry(f.second().id(), session.id()));
        assertThat(examLocks.lockedQuestions(UUID.randomUUID())).isEmpty();
        sessions.submit(session.id());
        assertThat(examLocks.lockedQuestions(dani)).containsKey(f.first().id());

        Session submitted = sessions.findById(session.id()).orElseThrow();
        sessions.complete(submitted.complete(Map.of(), T0.plusSeconds(60)));
        entityManager.clear();

        assertThat(examLocks.lockedQuestions(dani)).isEmpty();
    }

    @Test
    void aLessonLocksNothingAndAFailedSimulationUnlocks() {
        Fixture f = fixture();
        UUID enzo = user("enzo");
        Session failed = open(f, enzo);
        sessions.submit(failed.id());
        sessions.fail(sessions.findById(failed.id()).orElseThrow().fail("x", T0.plusSeconds(60)));
        sessions.create(
                Session.lesson(
                        UUID.randomUUID(),
                        enzo,
                        f.area().id(),
                        Level.JUNIOR,
                        f.tag(),
                        List.of(
                                f.first().id(),
                                f.second().id(),
                                questions
                                        .save(
                                                SessionTestData.question(
                                                        "zz-r6-" + SessionTestData.suffix(),
                                                        f.area(),
                                                        Level.JUNIOR,
                                                        true,
                                                        f.tag()))
                                        .id()),
                        T0.plusSeconds(120)));
        entityManager.clear();

        assertThat(examLocks.lockedQuestions(enzo)).isEmpty();
        assertThat(examLocks.lockedChallenges(enzo)).isEmpty();
    }

    @Test
    void theSimulationInProgressWinsOverASubmittedOneWithTheSameProblems() {
        Fixture f = fixture();
        UUID fabi = user("fabi");
        Session inProgress = open(f, fabi);
        Session submitted =
                sessions.create(
                        Session.simulation(
                                        UUID.randomUUID(),
                                        fabi,
                                        f.area().id(),
                                        Level.JUNIOR,
                                        f.area().sessionConfig(),
                                        List.of(f.first().id(), f.second().id()),
                                        List.of(f.challenge().id()),
                                        T0.plusSeconds(3_600))
                                .submit());
        entityManager.clear();

        assertThat(sessions.findById(submitted.id()).orElseThrow().status())
                .isEqualTo(SessionStatus.SUBMITTED);
        assertThat(examLocks.lockedChallenges(fabi))
                .containsExactly(Map.entry(f.challenge().id(), inProgress.id()));
        assertThat(examLocks.lockedQuestions(fabi))
                .containsOnly(
                        Map.entry(f.first().id(), inProgress.id()),
                        Map.entry(f.second().id(), inProgress.id()));

        UUID gabi = user("gabi");
        Session old = open(f, gabi);
        sessions.submit(old.id());
        Session current = open(f, gabi);
        entityManager.clear();

        assertThat(examLocks.lockedChallenges(gabi))
                .containsExactly(Map.entry(f.challenge().id(), current.id()));
        assertThat(examLocks.lockedQuestions(gabi))
                .containsOnly(
                        Map.entry(f.first().id(), current.id()),
                        Map.entry(f.second().id(), current.id()));
    }

    @Test
    void sessionSubmissionsStayOutOfThePracticeReadings() {
        Fixture f = fixture();
        UUID bia = user("bia");
        Session session = open(f, bia);
        CodeSubmission failed = finished(f, bia, session.id(), 1);
        entityManager.clear();

        assertThat(attempts.findByUser(bia)).isEmpty();
        assertThat(attempts.findByChallenges(List.of(f.challenge().id()))).isEmpty();
        assertThat(
                        submissions.findByUserAndChallenge(
                                bia, f.challenge().id(), SubmissionKind.SUBMIT))
                .isEmpty();
        assertThat(submissions.findBySession(session.id()))
                .extracting(CodeSubmission::id)
                .containsExactly(failed.id());
        assertThat(submissions.findBySession(session.id()).get(0).sessionId())
                .isEqualTo(session.id());
        assertThat(progress.answeredBy(bia)).isEmpty();
    }
}
