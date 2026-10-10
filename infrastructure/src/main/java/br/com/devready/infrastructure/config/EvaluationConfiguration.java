package br.com.devready.infrastructure.config;

import br.com.devready.application.evaluation.port.LanguageRuntimes;
import br.com.devready.application.evaluation.port.SubmissionQueue;
import br.com.devready.infrastructure.evaluation.ConfiguredLanguageRuntimes;
import br.com.devready.infrastructure.evaluation.EvaluationProperties;
import br.com.devready.infrastructure.evaluation.messaging.EvaluationMessaging;
import br.com.devready.infrastructure.evaluation.messaging.RabbitSubmissionQueue;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EvaluationProperties.class)
public class EvaluationConfiguration {

    @Bean
    Declarables evaluationTopology() {
        TopicExchange exchange = new TopicExchange(EvaluationMessaging.EXCHANGE, true, false);
        Queue deadLetters = QueueBuilder.durable(EvaluationMessaging.DEAD_LETTER_QUEUE).build();
        Queue submissions =
                QueueBuilder.durable(EvaluationMessaging.QUEUE)
                        .deadLetterExchange("")
                        .deadLetterRoutingKey(EvaluationMessaging.DEAD_LETTER_QUEUE)
                        .build();
        return new Declarables(
                exchange,
                deadLetters,
                submissions,
                BindingBuilder.bind(submissions)
                        .to(exchange)
                        .with(EvaluationMessaging.ROUTING_KEY));
    }

    @Bean
    SubmissionQueue submissionQueue(RabbitTemplate rabbit) {
        return new RabbitSubmissionQueue(rabbit);
    }

    @Bean
    LanguageRuntimes languageRuntimes(EvaluationProperties properties) {
        return new ConfiguredLanguageRuntimes(properties);
    }
}
