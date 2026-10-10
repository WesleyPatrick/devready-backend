package br.com.devready.infrastructure.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.identity.User;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.identity.persistence.JpaUserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({PostgresTestcontainersConfiguration.class, JpaUserRepository.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SpringTransactionsTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JpaUserRepository users;

    private User someone() {
        UUID id = UUID.randomUUID();
        return User.register(id, "tx-" + id + "@example.com", "Tx", "hash", T0);
    }

    @Test
    void everythingInsideCommitsTogether() {
        SpringTransactions transactions = new SpringTransactions(transactionManager);
        User first = someone();
        User second = someone();

        transactions.inTransaction(
                () -> {
                    users.save(first);
                    return users.save(second);
                });

        assertThat(users.findById(first.id())).isPresent();
        assertThat(users.findById(second.id())).isPresent();
    }

    @Test
    void aFailureInsideUndoesWhatWasSaved() {
        SpringTransactions transactions = new SpringTransactions(transactionManager);
        User first = someone();

        assertThatThrownBy(
                        () ->
                                transactions.inTransaction(
                                        () -> {
                                            users.save(first);
                                            throw new IllegalStateException("no meio");
                                        }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("no meio");

        assertThat(users.findById(first.id())).isEmpty();
    }
}
