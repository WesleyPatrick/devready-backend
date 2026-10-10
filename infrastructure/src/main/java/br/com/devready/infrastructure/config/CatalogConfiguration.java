package br.com.devready.infrastructure.config;

import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.CatalogSeedSource;
import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.application.catalog.port.TagRepository;
import br.com.devready.application.catalog.usecase.ListAreasUseCase;
import br.com.devready.application.catalog.usecase.SeedCatalogUseCase;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.infrastructure.catalog.seed.CatalogSeedRunner;
import br.com.devready.infrastructure.catalog.seed.YamlCatalogSeedSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SeedProperties.class)
public class CatalogConfiguration {

    @Bean
    ListAreasUseCase listAreas(
            AreaRepository areas, QuestionRepository questions, ChallengeRepository challenges) {
        return new ListAreasUseCase(areas, questions, challenges);
    }

    @Bean
    CatalogSeedSource catalogSeedSource(SeedProperties properties) {
        return new YamlCatalogSeedSource(properties.location());
    }

    @Bean
    SeedCatalogUseCase seedCatalog(
            CatalogSeedSource source,
            AreaRepository areas,
            TagRepository tags,
            QuestionRepository questions,
            ChallengeRepository challenges,
            IdGenerator ids) {
        return new SeedCatalogUseCase(source, areas, tags, questions, challenges, ids);
    }

    @Bean
    @ConditionalOnProperty(
            name = "devready.seed.enabled",
            havingValue = "true",
            matchIfMissing = true)
    CatalogSeedRunner catalogSeedRunner(SeedCatalogUseCase seedCatalog) {
        return new CatalogSeedRunner(seedCatalog);
    }
}
