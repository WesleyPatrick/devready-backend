package br.com.devready.worker;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.evaluation.port.CodeExecutor;
import br.com.devready.infrastructure.evaluation.judge0.Judge0CodeExecutor;
import br.com.devready.worker.evaluation.SubmissionListener;
import br.com.devready.worker.session.SessionExpiryScheduler;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class WorkerSmokeTest {

    @Autowired private ApplicationContext context;

    @Test
    void startsWithoutWebServerAndWithRabbitTemplate() {
        assertThat(context.getEnvironment().getProperty("spring.main.web-application-type"))
                .isEqualTo("none");
        assertThat(context.getBean(RabbitTemplate.class)).isNotNull();
        assertThat(context.getBean(SubmissionListener.class)).isNotNull();
        assertThat(context.getBean(SessionExpiryScheduler.class)).isNotNull();
        assertThat(context.getBean(CodeExecutor.class)).isInstanceOf(Judge0CodeExecutor.class);
    }
}
