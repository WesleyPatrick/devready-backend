package br.com.devready.infrastructure.identity.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.identity.dto.ChangeUserAccessCommand;
import br.com.devready.application.identity.exception.NotAnActiveAdminException;
import br.com.devready.application.identity.port.RefreshTokenStore;
import br.com.devready.application.identity.usecase.ChangeUserAccessUseCase;
import br.com.devready.domain.identity.Role;
import br.com.devready.domain.identity.User;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.shared.SpringTransactions;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
@Import({PostgresTestcontainersConfiguration.class, JpaUserRepository.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AdminLockTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);
    private static final long WAIT_SECONDS = 10;
    private static final ChangeUserAccessCommand DEMOTE =
            new ChangeUserAccessCommand(null, Role.CANDIDATE);

    private static final RefreshTokenStore NO_SESSIONS =
            new RefreshTokenStore() {
                @Override
                public String issue(UUID userId) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public ConsumeResult consume(String token) {
                    return ConsumeResult.unknown();
                }

                @Override
                public void revoke(String token) {}

                @Override
                public void revokeAll(UUID userId) {}
            };

    @Autowired private JpaUserRepository users;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private DataSource dataSource;

    private final ExecutorService threads = Executors.newFixedThreadPool(2);
    private final CountDownLatch holding = new CountDownLatch(1);
    private final CountDownLatch release = new CountDownLatch(1);
    private ChangeUserAccessUseCase change;

    @BeforeEach
    void onlyTheAdminsOfEachTestAreActive() {
        change =
                new ChangeUserAccessUseCase(
                        users, NO_SESSIONS, new SpringTransactions(transactionManager));
        new JdbcTemplate(dataSource)
                .update("update users set active = false where role = 'ADMIN' and active");
    }

    @AfterEach
    void stopThreads() {
        release.countDown();
        threads.shutdownNow();
    }

    private User admin(String name) {
        UUID id = UUID.randomUUID();
        User user = User.register(id, name + "-" + id + "@example.com", name, "hash", T0);
        user.promoteToAdmin();
        return users.save(user);
    }

    private <T> Future<T> holdingTheTransaction(Supplier<T> work) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        Callable<T> task =
                () ->
                        template.execute(
                                status -> {
                                    T result = work.get();
                                    holding.countDown();
                                    awaitRelease();
                                    return result;
                                });
        return threads.submit(task);
    }

    private <T> Future<T> later(Supplier<T> work) {
        Callable<T> task = work::get;
        return threads.submit(task);
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
    void ofTheLastTwoAdminsDemotingEachOtherAtTheSameTimeOnlyOneIsDemoted() throws Exception {
        User ana = admin("ana");
        User bia = admin("bia");

        Future<User> first =
                holdingTheTransaction(() -> change.execute(bia.id(), ana.id(), DEMOTE));
        assertThat(holding.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        Future<User> second = later(() -> change.execute(ana.id(), bia.id(), DEMOTE));

        assertThat(someoneWaitsForALock()).as("a segunda espera a trava dos admins").isTrue();
        assertThat(second).isNotDone();
        release.countDown();

        assertThat(first.get(WAIT_SECONDS, TimeUnit.SECONDS).role()).isEqualTo(Role.CANDIDATE);
        assertThatThrownBy(() -> second.get(WAIT_SECONDS, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(NotAnActiveAdminException.class);
        assertThat(users.findById(ana.id()).orElseThrow().role()).isEqualTo(Role.CANDIDATE);
        assertThat(users.findById(bia.id()).orElseThrow().isActiveAdmin()).isTrue();
    }

    @Test
    void theSameAdminDemotedTwiceAtTheSameTimeIsReadAfterTheLock() throws Exception {
        User ana = admin("ana");
        User bia = admin("bia");

        Future<User> first =
                holdingTheTransaction(() -> change.execute(bia.id(), ana.id(), DEMOTE));
        assertThat(holding.await(WAIT_SECONDS, TimeUnit.SECONDS)).isTrue();
        Future<User> second = later(() -> change.execute(bia.id(), ana.id(), DEMOTE));

        assertThat(someoneWaitsForALock()).as("a segunda espera a trava dos admins").isTrue();
        release.countDown();

        assertThat(first.get(WAIT_SECONDS, TimeUnit.SECONDS).role()).isEqualTo(Role.CANDIDATE);
        assertThat(second.get(WAIT_SECONDS, TimeUnit.SECONDS).role()).isEqualTo(Role.CANDIDATE);
        assertThat(users.findById(bia.id()).orElseThrow().isActiveAdmin()).isTrue();
    }
}
