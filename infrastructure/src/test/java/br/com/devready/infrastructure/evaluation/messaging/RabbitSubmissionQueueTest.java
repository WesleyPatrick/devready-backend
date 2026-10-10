package br.com.devready.infrastructure.evaluation.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.devready.application.evaluation.port.SubmissionQueue;
import br.com.devready.infrastructure.InfrastructureTestApplication;
import br.com.devready.infrastructure.TestcontainersConfiguration;
import com.rabbitmq.client.GetResponse;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = InfrastructureTestApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class RabbitSubmissionQueueTest {

    @Autowired private SubmissionQueue queue;
    @Autowired private RabbitTemplate rabbit;
    @Autowired private PlatformTransactionManager transactions;

    @BeforeEach
    void drain() {
        while (rabbit.receive(EvaluationMessaging.QUEUE, 100) != null) {}
        while (rabbit.receive(EvaluationMessaging.DEAD_LETTER_QUEUE, 100) != null) {}
    }

    @Test
    void outsideATransactionTheIdGoesToTheQueueRightAway() {
        UUID id = UUID.randomUUID();

        queue.publish(id);

        assertThat(rabbit.receiveAndConvert(EvaluationMessaging.QUEUE, 5_000))
                .isEqualTo(id.toString());
    }

    @Test
    void insideATransactionTheIdOnlyGoesAfterTheCommit() {
        UUID id = UUID.randomUUID();

        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> {
                            queue.publish(id);
                            assertThat(rabbit.receiveAndConvert(EvaluationMessaging.QUEUE, 300))
                                    .isNull();
                        });

        assertThat(rabbit.receiveAndConvert(EvaluationMessaging.QUEUE, 5_000))
                .isEqualTo(id.toString());
    }

    @Test
    void aRolledBackTransactionPublishesNothing() {
        new TransactionTemplate(transactions)
                .executeWithoutResult(
                        status -> {
                            queue.publish(UUID.randomUUID());
                            status.setRollbackOnly();
                        });

        assertThat(rabbit.receiveAndConvert(EvaluationMessaging.QUEUE, 1_000)).isNull();
    }

    @Test
    void aRejectedMessageGoesToTheDeadLetterQueue() {
        rabbit.convertAndSend(
                EvaluationMessaging.EXCHANGE, EvaluationMessaging.ROUTING_KEY, "rejeitada");

        await().atMost(Duration.ofSeconds(5))
                .until(
                        () ->
                                rabbit.execute(
                                        channel -> {
                                            GetResponse response =
                                                    channel.basicGet(
                                                            EvaluationMessaging.QUEUE, false);
                                            if (response == null) {
                                                return false;
                                            }
                                            channel.basicReject(
                                                    response.getEnvelope().getDeliveryTag(), false);
                                            return true;
                                        }));

        assertThat(rabbit.receiveAndConvert(EvaluationMessaging.DEAD_LETTER_QUEUE, 5_000))
                .isEqualTo("rejeitada");
    }
}
