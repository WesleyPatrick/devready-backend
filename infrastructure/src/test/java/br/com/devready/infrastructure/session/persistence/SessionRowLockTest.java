package br.com.devready.infrastructure.session.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.Tag;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionStatus;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.catalog.persistence.JpaAreaRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaChallengeRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaQuestionRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaTagRepository;
import br.com.devready.infrastructure.identity.persistence.JpaUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    PostgresTestcontainersConfiguration.class,
    JpaAreaRepository.class,
    JpaTagRepository.class,
    JpaQuestionRepository.class,
    JpaChallengeRepository.class,
    JpaUserRepository.class,
    JpaSessionRepository.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SessionRowLockTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);
    private static final long WAIT_SECONDS = 10;

    @Autowired private JpaAreaRepository areas;
    @Autowired private JpaTagRepository tags;
    @Autowired private JpaQuestionRepository questions;
    @Autowired private JpaChallengeRepository challenges;
    @Autowired private JpaUserRepository users;
    @Autowired private JpaSessionRepository sessions;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private DataSource dataSource;

    private final ExecutorService threads = Executors.newFixedThreadPool(2);
    private final CountDownLatch holding = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private final List<String> events = new CopyOnWriteArrayList<>();
    private UUID rightOption;

    @AfterEach
    void stopThreads() {
        release.countDown();
        threads.shutdownNow();
    }

    private Session openSimulation() {
        String s = SessionTestData.suffix();
        Area area = areas.save(SessionTestData.area("zz-trava-" + s));
        tags.save(new Tag(UUID.randomUUID(), "zz-trava-" + s, "Trava"));
        Question first =
                questions.save(
                        SessionTestData.question(
                                "zz-trava-q1-" + s, area, Level.JUNIOR, true, "zz-trava-" + s));
        Question second =
                questions.save(
                        SessionTestData.question(
                                "zz-trava-q2-" + s, area, Level.JUNIOR, true, "zz-trava-" + s));
        Challenge challenge =
                challenges.save(
                        SessionTestData.challenge(
                                "zz-trava-c-" + s, area, Level.JUNIOR, true, "zz-trava-" + s));
        rightOption = first.correctOption().id();
        UUID user = UUID.randomUUID();
        users.save(User.register(user, "trava-" + user + "@example.com", "Trava", "hash", T0));
        return sessions.create(
                Session.simulation(
                        UUID.randomUUID(),
                        user,
                        area.id(),
                        Level.JUNIOR,
                        area.sessionConfig(),
                        List.of(first.id(), second.id()),
                        List.of(challenge.id()),
                        T0));
    }

    private Future<Boolean> holdingTheTransaction(String event, BooleanSupplier work) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return threads.submit(
                () ->
                        template.execute(
                                status -> {
                                    boolean changed = work.getAsBoolean();
                                    holding.countDown();
                                    awaitRelease();
                                    events.add(event);
                                    return changed;
                                }));
    }

    private Future<Boolean> later(String event, BooleanSupplier work) {
        return threads.submit(
                () -> {
                    boolean changed = work.getAsBoolean();
                    events.add(event);
                    return changed;
                });
    }

    private void awaitRelease() {
        try {
            if (!release.await(WAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException("o teste não soltou a transação");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private boolean someoneWaitsForALock() throws InterruptedException {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(WAIT_SECONDS);
        while (System.nanoTime() < until) {
            Integer waiting =
                    jdbc.queryForObject(
                            "select count(*) from pg_stat_activity"
                                    + " where datname = current_database()"
                                    + " and wait_event_type = 'Lock'",
                            Integer.class);
            if (waiting != null && waiting > 0) {
                return true;
            }
            Thread.sleep(20);
        }
        return false;
    }

    @Test
    void aSubmissionNotYetCommittedHoldsTheSubmitUntilTheCommit() throws Exception {
        Session session = openSimulation();
        Future<Boolean> sending =
                holdingTheTransaction(
                        "envio gravado", () -> sessions.useSubmission(session.id(), 1, 5));
        assertThat(holding.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

        Future<Boolean> submitting = later("entregue", () -> sessions.submit(session.id()));

        assertThat(someoneWaitsForALock()).as("a entrega espera o commit do envio").isTrue();
        assertThat(submitting).isNotDone();
        release.countDown();
        assertThat(sending.get(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(submitting.get(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(events).containsExactly("envio gravado", "entregue");
        Session read = sessions.findById(session.id()).orElseThrow();
        assertThat(read.status()).isEqualTo(SessionStatus.SUBMITTED);
        assertThat(read.challenges().get(0).submissionsUsed()).isEqualTo(1);
    }

    @Test
    void anAnswerNotYetCommittedHoldsTheSubmitUntilTheCommit() throws Exception {
        Session session = openSimulation();
        Future<Boolean> answering =
                holdingTheTransaction(
                        "resposta gravada",
                        () -> sessions.answer(session.id(), 1, rightOption, true, true));
        assertThat(holding.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

        Future<Boolean> submitting = later("entregue", () -> sessions.submit(session.id()));

        assertThat(someoneWaitsForALock()).as("a entrega espera o commit da resposta").isTrue();
        assertThat(submitting).isNotDone();
        release.countDown();
        assertThat(answering.get(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(submitting.get(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(events).containsExactly("resposta gravada", "entregue");
        assertThat(sessions.findById(session.id()).orElseThrow().question(1).orElseThrow().hit())
                .isTrue();
    }

    @Test
    void aSubmissionAfterTheSubmitWaitsForItAndFindsTheSessionClosed() throws Exception {
        Session session = openSimulation();
        Future<Boolean> submitting =
                holdingTheTransaction("entregue", () -> sessions.submit(session.id()));
        assertThat(holding.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();

        Future<Boolean> sending =
                later("envio recusado", () -> sessions.useSubmission(session.id(), 1, 5));

        assertThat(someoneWaitsForALock()).as("o envio espera o commit da entrega").isTrue();
        assertThat(sending).isNotDone();
        release.countDown();
        assertThat(submitting.get(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        assertThat(sending.get(WAIT_SECONDS, TimeUnit.SECONDS)).isFalse();
        assertThat(events).containsExactly("entregue", "envio recusado");
        assertThat(sessions.findById(session.id()).orElseThrow().challenges().get(0))
                .extracting(c -> c.submissionsUsed())
                .isEqualTo(0);
    }
}
