package br.com.devready.infrastructure.catalog.seed;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.application.catalog.usecase.ListAreasUseCase;
import br.com.devready.application.catalog.usecase.SeedCatalogUseCase;
import br.com.devready.infrastructure.InfrastructureTestApplication;
import br.com.devready.infrastructure.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
        classes = InfrastructureTestApplication.class,
        properties = {
            "devready.seed.enabled=true",
            "devready.seed.location=classpath:seed-fixtures"
        })
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class CatalogSeedRunnerTest {

    @Autowired private ListAreasUseCase listAreas;
    @Autowired private SeedCatalogUseCase seedCatalog;
    @Autowired private QuestionRepository questions;
    @Autowired private ChallengeRepository challenges;

    @Test
    void seedRanOnStartupAndRunningAgainInsertsNothing() {
        assertThat(listAreas.execute()).extracting(a -> a.area().slug()).contains("backend");
        assertThat(questions.existsBySlug("backend-junior-http-idempotency")).isTrue();
        assertThat(challenges.existsBySlug("two-sum")).isTrue();

        assertThat(seedCatalog.execute().total()).isZero();
    }
}
