package br.com.devready.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {

    private static final String DOMAIN = "br.com.devready.domain..";
    private static final String APPLICATION = "br.com.devready.application..";
    private static final String INFRASTRUCTURE = "br.com.devready.infrastructure..";
    private static final String API = "br.com.devready.api..";
    private static final String WORKER = "br.com.devready.worker..";

    private static final JavaClasses CLASSES =
            new ClassFileImporter()
                    .withImportOption(new ImportOption.DoNotIncludeTests())
                    .importPackages("br.com.devready");

    @Test
    void domainDoesNotDependOnFrameworks() {
        noClasses()
                .that()
                .resideInAPackage(DOMAIN)
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "org.springframework..",
                        "jakarta.persistence..",
                        "jakarta.validation..",
                        "com.fasterxml..",
                        "tools.jackson..",
                        "org.hibernate..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void applicationDependsOnlyOnDomainAndJdk() {
        classes()
                .that()
                .resideInAPackage(APPLICATION)
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage(DOMAIN, APPLICATION, "java..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void layersOnlyPointInward() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .withOptionalLayers(true)
                .layer("Domain")
                .definedBy(DOMAIN)
                .layer("Application")
                .definedBy(APPLICATION)
                .layer("Infrastructure")
                .definedBy(INFRASTRUCTURE)
                .layer("Api")
                .definedBy(API)
                .layer("Worker")
                .definedBy(WORKER)
                .whereLayer("Domain")
                .mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Api", "Worker")
                .whereLayer("Application")
                .mayOnlyBeAccessedByLayers("Infrastructure", "Api", "Worker")
                .whereLayer("Infrastructure")
                .mayOnlyBeAccessedByLayers("Api", "Worker")
                .whereLayer("Api")
                .mayNotBeAccessedByAnyLayer()
                .whereLayer("Worker")
                .mayNotBeAccessedByAnyLayer()
                .check(CLASSES);
    }

    @Test
    void apiAndWorkerDoNotKnowEachOther() {
        noClasses()
                .that()
                .resideInAPackage(API)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(WORKER)
                .allowEmptyShould(true)
                .check(CLASSES);
        noClasses()
                .that()
                .resideInAPackage(WORKER)
                .should()
                .dependOnClassesThat()
                .resideInAPackage(API)
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void theTrackDoesNotKnowPracticeNorTheEngine() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "br.com.devready.domain.career..", "br.com.devready.application.career..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.devready.domain.practice..",
                        "br.com.devready.application.practice..",
                        "br.com.devready.domain.evaluation..",
                        "br.com.devready.application.evaluation..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void theEngineDoesNotKnowPracticeNorTheTrack() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "br.com.devready.domain.evaluation..",
                        "br.com.devready.application.evaluation..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.devready.domain.practice..",
                        "br.com.devready.application.practice..",
                        "br.com.devready.domain.career..",
                        "br.com.devready.application.career..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void theTrackAndTheEngineDoNotKnowSessionsNorAchievements() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "br.com.devready.domain.career..",
                        "br.com.devready.application.career..",
                        "br.com.devready.domain.evaluation..",
                        "br.com.devready.application.evaluation..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.devready.domain.session..",
                        "br.com.devready.application.session..",
                        "br.com.devready.domain.gamification..",
                        "br.com.devready.application.gamification..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void practiceDoesNotKnowSessionsNorAchievements() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "br.com.devready.domain.practice..",
                        "br.com.devready.application.practice..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.devready.domain.session..",
                        "br.com.devready.application.session..",
                        "br.com.devready.domain.gamification..",
                        "br.com.devready.application.gamification..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void sessionsDoNotKnowAchievements() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "br.com.devready.domain.session..", "br.com.devready.application.session..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "br.com.devready.domain.gamification..",
                        "br.com.devready.application.gamification..")
                .allowEmptyShould(true)
                .check(CLASSES);
    }

    @Test
    void importsTheApplicationClasses() {
        assertThat(CLASSES.contain(br.com.devready.domain.catalog.Level.class)).isTrue();
        assertThat(CLASSES.contain(br.com.devready.api.DevReadyApiApplication.class)).isTrue();
        assertThat(CLASSES.contain(br.com.devready.worker.DevReadyWorkerApplication.class))
                .isTrue();
    }
}
