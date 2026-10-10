package br.com.devready.application.career.usecase;

import static br.com.devready.application.career.fake.CareerFixtures.company;
import static br.com.devready.application.career.fake.CareerFixtures.stack;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.dto.SaveCareerProfileCommand;
import br.com.devready.application.career.exception.UnknownCompanyException;
import br.com.devready.application.career.exception.UnknownStackException;
import br.com.devready.application.career.fake.InMemoryCareerProfileRepository;
import br.com.devready.application.career.fake.InMemoryCompanyRepository;
import br.com.devready.application.career.fake.InMemoryStackRepository;
import br.com.devready.domain.career.CareerException;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SaveCareerProfileUseCaseTest {

    private final InMemoryCompanyRepository companies = new InMemoryCompanyRepository();
    private final InMemoryStackRepository stacks = new InMemoryStackRepository();
    private final InMemoryCareerProfileRepository profiles = new InMemoryCareerProfileRepository();
    private final SaveCareerProfileUseCase save =
            new SaveCareerProfileUseCase(profiles, companies, stacks);
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void seed() {
        companies.save(company("nubank", "Nubank", CompanyGroup.TECH_BRASIL));
        companies.save(company("google", "Google", CompanyGroup.BIG_TECH));
        companies.save(
                new Company(
                        UUID.randomUUID(),
                        "velha",
                        "Velha",
                        CompanyGroup.BIG_TECH,
                        "",
                        List.of(),
                        false));
        stacks.save(stack("java-spring", "Java e Spring", "backend"));
    }

    @Test
    void savesAndReturnsCompaniesInTheChosenOrder() {
        var view =
                save.execute(
                        new SaveCareerProfileCommand(
                                user, List.of("nubank", "google"), "java-spring", Level.JUNIOR));

        assertThat(view.companies()).extracting(Company::slug).containsExactly("nubank", "google");
        assertThat(view.stack().slug()).isEqualTo("java-spring");
        assertThat(profiles.findByUserId(user).orElseThrow().companySlugs())
                .containsExactly("nubank", "google");
    }

    @Test
    void replacesAnExistingProfile() {
        save.execute(
                new SaveCareerProfileCommand(user, List.of("nubank"), "java-spring", Level.JUNIOR));
        save.execute(
                new SaveCareerProfileCommand(user, List.of("google"), "java-spring", Level.PLENO));

        var stored = profiles.findByUserId(user).orElseThrow();
        assertThat(stored.companySlugs()).containsExactly("google");
        assertThat(stored.seniority()).isEqualTo(Level.PLENO);
    }

    @Test
    void rejectsUnknownOrInactiveCompaniesNamingThem() {
        assertThatThrownBy(
                        () ->
                                save.execute(
                                        new SaveCareerProfileCommand(
                                                user,
                                                List.of("nubank", "velha", "xpto"),
                                                "java-spring",
                                                Level.JUNIOR)))
                .isInstanceOf(UnknownCompanyException.class)
                .hasMessageContaining("velha")
                .hasMessageContaining("xpto");
        assertThat(profiles.findByUserId(user)).isEmpty();
    }

    @Test
    void rejectsUnknownStack() {
        assertThatThrownBy(
                        () ->
                                save.execute(
                                        new SaveCareerProfileCommand(
                                                user, List.of("nubank"), "cobol", Level.JUNIOR)))
                .isInstanceOf(UnknownStackException.class)
                .hasMessageContaining("cobol");
        assertThat(profiles.findByUserId(user)).isEmpty();
    }

    @Test
    void domainRulesStillApply() {
        assertThatThrownBy(
                        () ->
                                save.execute(
                                        new SaveCareerProfileCommand(
                                                user, List.of(), "java-spring", Level.JUNIOR)))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(
                        () ->
                                save.execute(
                                        new SaveCareerProfileCommand(
                                                user, null, "java-spring", Level.JUNIOR)))
                .isInstanceOf(CareerException.class);
    }
}
