package br.com.devready.infrastructure.config;

import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.application.career.port.CareerSeedSource;
import br.com.devready.application.career.port.CompanyRepository;
import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.application.career.port.QuestionProgressReader;
import br.com.devready.application.career.port.StackRepository;
import br.com.devready.application.career.port.TrackRepository;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.career.usecase.GetCareerProfileUseCase;
import br.com.devready.application.career.usecase.GetLearningTrackUseCase;
import br.com.devready.application.career.usecase.GetTrackCompetencyUseCase;
import br.com.devready.application.career.usecase.ListOnboardingOptionsUseCase;
import br.com.devready.application.career.usecase.SaveCareerProfileUseCase;
import br.com.devready.application.career.usecase.SeedCareerUseCase;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.PracticeCatalog;
import br.com.devready.application.catalog.port.TagRepository;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.infrastructure.career.seed.CareerSeedRunner;
import br.com.devready.infrastructure.career.seed.YamlCareerSeedSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class CareerConfiguration {

    @Bean
    ListOnboardingOptionsUseCase listOnboardingOptions(
            CompanyRepository companies, StackRepository stacks) {
        return new ListOnboardingOptionsUseCase(companies, stacks);
    }

    @Bean
    GetCareerProfileUseCase getCareerProfile(
            CareerProfileRepository profiles, CompanyRepository companies, StackRepository stacks) {
        return new GetCareerProfileUseCase(profiles, companies, stacks);
    }

    @Bean
    SaveCareerProfileUseCase saveCareerProfile(
            CareerProfileRepository profiles, CompanyRepository companies, StackRepository stacks) {
        return new SaveCareerProfileUseCase(profiles, companies, stacks);
    }

    @Bean
    CareerSeedSource careerSeedSource(SeedProperties properties) {
        return new YamlCareerSeedSource(properties.location());
    }

    @Bean
    TrackAssembler trackAssembler(
            CareerProfileRepository profiles,
            CompanyRepository companies,
            StackRepository stacks,
            CompetencyRepository competencies,
            TrackRepository tracks,
            AreaRepository areas,
            PracticeCatalog catalog,
            QuestionProgressReader progress) {
        return new TrackAssembler(
                profiles, companies, stacks, competencies, tracks, areas, catalog, progress);
    }

    @Bean
    GetLearningTrackUseCase getLearningTrack(TrackAssembler assembler) {
        return new GetLearningTrackUseCase(assembler);
    }

    @Bean
    GetTrackCompetencyUseCase getTrackCompetency(
            TrackAssembler assembler, AreaRepository areas, PracticeCatalog catalog) {
        return new GetTrackCompetencyUseCase(assembler, areas, catalog);
    }

    @Bean
    SeedCareerUseCase seedCareer(
            CareerSeedSource source,
            CompanyRepository companies,
            StackRepository stacks,
            AreaRepository areas,
            TagRepository tags,
            CompetencyRepository competencies,
            TrackRepository tracks,
            IdGenerator ids) {
        return new SeedCareerUseCase(
                source, companies, stacks, areas, tags, competencies, tracks, ids);
    }

    @Bean
    @ConditionalOnProperty(
            name = "devready.seed.enabled",
            havingValue = "true",
            matchIfMissing = true)
    CareerSeedRunner careerSeedRunner(
            SeedCareerUseCase seedCareer, PlatformTransactionManager transactionManager) {
        return new CareerSeedRunner(seedCareer, transactionManager);
    }
}
