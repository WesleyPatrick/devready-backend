package br.com.devready.worker.config;

import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.evaluation.port.CodeExecutor;
import br.com.devready.application.evaluation.port.HarnessBuilder;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.evaluation.usecase.ProcessSubmissionUseCase;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.application.shared.Sleeper;
import br.com.devready.infrastructure.evaluation.harness.TemplateHarnessBuilder;
import br.com.devready.infrastructure.evaluation.judge0.Judge0CodeExecutor;
import br.com.devready.infrastructure.evaluation.judge0.Judge0Properties;
import br.com.devready.infrastructure.shared.ThreadSleeper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(Judge0Properties.class)
public class EvaluationWorkerConfiguration {

    @Bean
    Sleeper sleeper() {
        return new ThreadSleeper();
    }

    @Bean
    HarnessBuilder harnessBuilder() {
        return new TemplateHarnessBuilder();
    }

    @Bean
    CodeExecutor codeExecutor(Judge0Properties properties, Sleeper sleeper) {
        return new Judge0CodeExecutor(properties, sleeper);
    }

    @Bean
    ProcessSubmissionUseCase processSubmission(
            SubmissionRepository submissions,
            ChallengeRepository challenges,
            HarnessBuilder harness,
            CodeExecutor executor,
            IdGenerator ids,
            Clock clock,
            Sleeper sleeper) {
        return new ProcessSubmissionUseCase(
                submissions, challenges, harness, executor, ids, clock, sleeper);
    }
}
