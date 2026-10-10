package br.com.devready.worker.evaluation;

import br.com.devready.application.evaluation.dto.ProcessOutcome;
import br.com.devready.application.evaluation.usecase.ProcessSubmissionUseCase;
import br.com.devready.application.gamification.usecase.AfterSubmissionUseCase;
import br.com.devready.infrastructure.evaluation.messaging.EvaluationMessaging;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class SubmissionListener {

    private static final Logger log = LoggerFactory.getLogger(SubmissionListener.class);

    private final ProcessSubmissionUseCase processSubmission;
    private final AfterSubmissionUseCase afterSubmission;
    private final RabbitTemplate rabbit;

    public SubmissionListener(
            ProcessSubmissionUseCase processSubmission,
            AfterSubmissionUseCase afterSubmission,
            RabbitTemplate rabbit) {
        this.processSubmission = processSubmission;
        this.afterSubmission = afterSubmission;
        this.rabbit = rabbit;
    }

    @RabbitListener(queues = EvaluationMessaging.QUEUE)
    public void onSubmissionQueued(String message) {
        UUID id;
        try {
            id = UUID.fromString(message.strip());
        } catch (IllegalArgumentException e) {
            log.warn("Mensagem de envio com id ilegível, descartada: {}", message);
            return;
        }
        ProcessOutcome outcome = processSubmission.execute(id);
        if (outcome == ProcessOutcome.FAILED) {
            rabbit.convertAndSend("", EvaluationMessaging.DEAD_LETTER_QUEUE, message);
        }
        if (outcome != ProcessOutcome.IGNORED) {
            try {
                afterSubmission.execute(id);
            } catch (RuntimeException e) {
                log.warn("Envio {} terminou, mas a correção ou as conquistas falharam", id, e);
            }
        }
    }
}
