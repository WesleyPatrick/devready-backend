package br.com.devready.infrastructure.career.seed;

import br.com.devready.application.career.dto.CareerSeedReport;
import br.com.devready.application.career.usecase.SeedCareerUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

public class CareerSeedRunner implements ApplicationRunner, Ordered {

    private static final Logger log = LoggerFactory.getLogger(CareerSeedRunner.class);

    private final SeedCareerUseCase seedCareer;
    private final TransactionTemplate transaction;

    public CareerSeedRunner(
            SeedCareerUseCase seedCareer, PlatformTransactionManager transactionManager) {
        this.seedCareer = seedCareer;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        CareerSeedReport report = transaction.execute(status -> seedCareer.execute());
        if (report.total() == 0) {
            log.info("Carreira já estava em dia; nada inserido, atualizado nem removido.");
        } else {
            log.info(
                    "Carreira: {} empresas inseridas e {} atualizadas; {} stacks inseridas e {}"
                            + " atualizadas; {} competências inseridas, {} atualizadas e {}"
                            + " removidas; {} trilhas inseridas, {} atualizadas e {} removidas.",
                    report.insertedCompanies(),
                    report.updatedCompanies(),
                    report.insertedStacks(),
                    report.updatedStacks(),
                    report.insertedCompetencies(),
                    report.updatedCompetencies(),
                    report.removedCompetencies(),
                    report.insertedTracks(),
                    report.updatedTracks(),
                    report.removedTracks());
        }
    }

    @Override
    public int getOrder() {
        return 2;
    }
}
