package br.com.devready.infrastructure.career.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.career.Stack;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.identity.User;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.catalog.persistence.JpaAreaRepository;
import br.com.devready.infrastructure.identity.persistence.JpaUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    PostgresTestcontainersConfiguration.class,
    JpaCompanyRepository.class,
    JpaStackRepository.class,
    JpaCareerProfileRepository.class,
    JpaAreaRepository.class,
    JpaUserRepository.class
})
class JpaCareerRepositoriesTest {

    private static final Instant NOW = Instant.ofEpochSecond(946_684_800L);

    @Autowired private JpaCompanyRepository companies;
    @Autowired private JpaStackRepository stacks;
    @Autowired private JpaCareerProfileRepository profiles;
    @Autowired private JpaAreaRepository areas;
    @Autowired private JpaUserRepository users;
    @Autowired private SpringDataCareerProfileRepository profileRows;
    @Autowired private TestEntityManager entityManager;

    @Test
    void savesCompanyWithCareerPagesAndUpdatesItInPlace() {
        Company nubank =
                company(
                        UUID.randomUUID(),
                        "nubank",
                        "",
                        List.of("https://jobs.ashbyhq.com/nubank", "https://nubank.com.br"),
                        true);
        companies.save(nubank);
        entityManager.clear();

        assertThat(companies.findBySlug("nubank")).contains(nubank);

        Company updated =
                company(
                        nubank.id(),
                        "nubank",
                        "Triagem, entrevista técnica e conversa com o time.",
                        List.of("https://nubank.com.br/carreiras"),
                        true);
        companies.save(updated);
        entityManager.clear();

        assertThat(companies.findBySlug("nubank")).contains(updated);
    }

    @Test
    void updatingACompanyRightAfterCreatingItInTheSameTransactionKeepsTheLastVersion() {
        Company first =
                company(
                        UUID.randomUUID(),
                        "stone",
                        "",
                        List.of("https://a.example", "https://b.example"),
                        true);
        companies.save(first);
        Company second =
                company(
                        first.id(),
                        "stone",
                        "Texto novo.",
                        List.of("https://c.example", "https://a.example", "https://d.example"),
                        true);
        companies.save(second);
        entityManager.clear();

        assertThat(companies.findBySlug("stone")).contains(second);
    }

    @Test
    void findAllActiveLeavesInactiveCompaniesOut() {
        Company active = company(UUID.randomUUID(), "ifood", "", List.of(), true);
        Company inactive = company(UUID.randomUUID(), "empresa-antiga", "", List.of(), false);
        companies.save(active);
        companies.save(inactive);
        entityManager.clear();

        assertThat(companies.findAllActive())
                .extracting(Company::slug)
                .contains("ifood")
                .doesNotContain("empresa-antiga");
        assertThat(companies.findBySlug("empresa-antiga")).map(Company::active).contains(false);
    }

    @Test
    void savesStackPointingToACatalogArea() {
        saveArea("backend");
        Stack javaSpring = new Stack(UUID.randomUUID(), "java-spring", "Java e Spring", "backend");
        stacks.save(javaSpring);
        entityManager.clear();

        assertThat(stacks.findBySlug("java-spring")).contains(javaSpring);
        assertThat(stacks.findAll()).contains(javaSpring);

        Stack renamed =
                new Stack(javaSpring.id(), "java-spring", "Java, Kotlin e Spring", "backend");
        stacks.save(renamed);
        entityManager.clear();

        assertThat(stacks.findBySlug("java-spring")).contains(renamed);
    }

    @Test
    void savesCareerProfileKeepingTheOrderAndReordersWithoutBreakingTheKey() {
        UUID userId = UUID.randomUUID();
        users.save(User.register(userId, "carreira@example.com", "Ana", "hash", NOW));
        companies.save(company(UUID.randomUUID(), "nubank", "", List.of(), true));
        companies.save(company(UUID.randomUUID(), "google", "", List.of(), true));
        companies.save(company(UUID.randomUUID(), "ifood", "", List.of(), true));
        saveArea("backend");
        stacks.save(new Stack(UUID.randomUUID(), "java-spring", "Java e Spring", "backend"));

        profiles.save(
                new CareerProfile(
                        userId, List.of("nubank", "google"), "java-spring", Level.JUNIOR));
        entityManager.clear();

        CareerProfile saved = profiles.findByUserId(userId).orElseThrow();
        assertThat(saved.companySlugs()).containsExactly("nubank", "google");
        assertThat(saved.stackSlug()).isEqualTo("java-spring");
        assertThat(saved.seniority()).isEqualTo(Level.JUNIOR);

        profiles.save(
                new CareerProfile(
                        userId, List.of("google", "nubank", "ifood"), "java-spring", Level.PLENO));
        entityManager.clear();

        CareerProfile reordered = profiles.findByUserId(userId).orElseThrow();
        assertThat(reordered.companySlugs()).containsExactly("google", "nubank", "ifood");
        assertThat(reordered.seniority()).isEqualTo(Level.PLENO);
        assertThat(profiles.findByUserId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void reorderingOnlyTheCompaniesStillMovesUpdatedAt() {
        UUID userId = UUID.randomUUID();
        users.save(User.register(userId, "ordem@example.com", "Bia", "hash", NOW));
        companies.save(company(UUID.randomUUID(), "nubank", "", List.of(), true));
        companies.save(company(UUID.randomUUID(), "google", "", List.of(), true));
        saveArea("backend");
        stacks.save(new Stack(UUID.randomUUID(), "java-spring", "Java e Spring", "backend"));
        profiles.save(
                new CareerProfile(
                        userId, List.of("nubank", "google"), "java-spring", Level.JUNIOR));
        entityManager
                .getEntityManager()
                .createNativeQuery(
                        "update career_profiles set updated_at = to_timestamp(0) where user_id = ?1")
                .setParameter(1, userId)
                .executeUpdate();
        entityManager.clear();

        profiles.save(
                new CareerProfile(
                        userId, List.of("google", "nubank"), "java-spring", Level.JUNIOR));
        entityManager.clear();

        assertThat(profileRows.findById(userId).orElseThrow().getUpdatedAt())
                .isAfter(Instant.EPOCH);
    }

    private void saveArea(String slug) {
        areas.save(
                new Area(
                        UUID.randomUUID(),
                        slug,
                        "Backend",
                        new SessionConfig(8, 2, 45, 0.5),
                        true));
    }

    private static Company company(
            UUID id, String slug, String about, List<String> careers, boolean active) {
        return new Company(
                id, slug, "Empresa " + slug, CompanyGroup.TECH_BRASIL, about, careers, active);
    }
}
