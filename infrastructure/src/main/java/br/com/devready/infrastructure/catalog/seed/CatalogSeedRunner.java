package br.com.devready.infrastructure.catalog.seed;

import br.com.devready.application.catalog.dto.SeedReport;
import br.com.devready.application.catalog.usecase.SeedCatalogUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;

public class CatalogSeedRunner implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeedRunner.class);

    private final SeedCatalogUseCase seedCatalog;

    public CatalogSeedRunner(SeedCatalogUseCase seedCatalog) {
        this.seedCatalog = seedCatalog;
    }

    @Override
    public void run(ApplicationArguments args) {
        SeedReport report = seedCatalog.execute();
        if (report.total() == 0) {
            log.info("Catálogo já estava completo; nada inserido nem atualizado.");
        } else {
            log.info(
                    "Catálogo: {} áreas, {} tags, {} questões e {} desafios inseridos; {} questões"
                            + " e {} desafios com título ou dificuldade atualizados.",
                    report.areas(),
                    report.tags(),
                    report.questions(),
                    report.challenges(),
                    report.updatedQuestions(),
                    report.updatedChallenges());
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
