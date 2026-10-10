package br.com.devready.infrastructure.evaluation.messaging;

import br.com.devready.application.evaluation.port.SubmissionQueue;
import java.util.UUID;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class RabbitSubmissionQueue implements SubmissionQueue {

    private final RabbitTemplate rabbit;

    public RabbitSubmissionQueue(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @Override
    public void publish(UUID submissionId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            send(submissionId);
                        }
                    });
            return;
        }
        send(submissionId);
    }

    private void send(UUID submissionId) {
        rabbit.convertAndSend(
                EvaluationMessaging.EXCHANGE,
                EvaluationMessaging.ROUTING_KEY,
                submissionId.toString());
    }
}
