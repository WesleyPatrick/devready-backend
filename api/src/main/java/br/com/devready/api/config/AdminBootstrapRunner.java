package br.com.devready.api.config;

import br.com.devready.application.identity.usecase.BootstrapAdminUseCase;
import br.com.devready.domain.identity.IdentityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(AdminProperties.class)
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final BootstrapAdminUseCase bootstrapAdmin;
    private final AdminProperties properties;

    public AdminBootstrapRunner(BootstrapAdminUseCase bootstrapAdmin, AdminProperties properties) {
        this.bootstrapAdmin = bootstrapAdmin;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            switch (bootstrapAdmin.execute(properties.email(), properties.password())) {
                case CREATED -> log.info("Administrador inicial criado: {}", properties.email());
                case EMAIL_BELONGS_TO_CANDIDATE ->
                        log.warn(
                                "DEVREADY_ADMIN_EMAIL ({}) já pertence a um candidato; nenhum"
                                        + " administrador foi criado e a conta não foi promovida.",
                                properties.email());
                case SKIPPED, ALREADY_ADMIN -> {}
            }
        } catch (IdentityException e) {
            throw new IllegalStateException(
                    "Administrador inicial recusado (DEVREADY_ADMIN_EMAIL/DEVREADY_ADMIN_PASSWORD): "
                            + e.getMessage()
                            + " Confira as duas variáveis no .env e troque os valores de exemplo.",
                    e);
        }
    }
}
