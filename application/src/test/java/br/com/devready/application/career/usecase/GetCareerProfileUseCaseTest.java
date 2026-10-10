package br.com.devready.application.career.usecase;

import static br.com.devready.application.career.fake.CareerFixtures.company;
import static br.com.devready.application.career.fake.CareerFixtures.stack;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.career.exception.UnknownStackException;
import br.com.devready.application.career.fake.InMemoryCareerProfileRepository;
import br.com.devready.application.career.fake.InMemoryCompanyRepository;
import br.com.devready.application.career.fake.InMemoryStackRepository;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GetCareerProfileUseCaseTest {

    private final InMemoryCompanyRepository companies = new InMemoryCompanyRepository();
    private final InMemoryStackRepository stacks = new InMemoryStackRepository();
    private final InMemoryCareerProfileRepository profiles = new InMemoryCareerProfileRepository();
    private final GetCareerProfileUseCase get =
            new GetCareerProfileUseCase(profiles, companies, stacks);
    private final UUID user = UUID.randomUUID();

    @BeforeEach
    void seed() {
        companies.save(company("nubank", "Nubank", CompanyGroup.TECH_BRASIL));
        companies.save(company("google", "Google", CompanyGroup.BIG_TECH));
        stacks.save(stack("java-spring", "Java e Spring", "backend"));
    }

    @Test
    void failsWhenThereIsNoProfile() {
        assertThatThrownBy(() -> get.execute(user))
                .isInstanceOf(CareerProfileNotFoundException.class)
                .hasMessage("Perfil de carreira ainda não preenchido.");
    }

    @Test
    void returnsCompaniesInTheSavedOrderWithStackAndSeniority() {
        profiles.save(
                new CareerProfile(user, List.of("nubank", "google"), "java-spring", Level.PLENO));

        var view = get.execute(user);

        assertThat(view.companies()).extracting(Company::slug).containsExactly("nubank", "google");
        assertThat(view.stack().slug()).isEqualTo("java-spring");
        assertThat(view.seniority()).isEqualTo(Level.PLENO);
    }

    @Test
    void keepsShowingACompanyThatWasDeactivatedLater() {
        profiles.save(
                new CareerProfile(user, List.of("google", "nubank"), "java-spring", Level.JUNIOR));
        Company google = companies.findBySlug("google").orElseThrow();
        companies.save(
                new Company(
                        google.id(),
                        google.slug(),
                        google.name(),
                        google.group(),
                        google.about(),
                        google.careers(),
                        false));

        var view = get.execute(user);

        assertThat(view.companies()).extracting(Company::slug).containsExactly("google", "nubank");
        assertThat(view.companies().get(0).active()).isFalse();
        assertThat(view.companies().get(1).active()).isTrue();
    }

    @Test
    void skipsACompanyThatNoLongerExists() {
        profiles.save(
                new CareerProfile(user, List.of("google", "nubank"), "java-spring", Level.JUNIOR));
        companies.remove("google");

        var view = get.execute(user);

        assertThat(view.companies()).extracting(Company::slug).containsExactly("nubank");
    }

    @Test
    void failsWhenTheStackNoLongerExists() {
        profiles.save(new CareerProfile(user, List.of("nubank"), "java-spring", Level.JUNIOR));
        stacks.remove("java-spring");

        assertThatThrownBy(() -> get.execute(user))
                .isInstanceOf(UnknownStackException.class)
                .hasMessageContaining("java-spring");
    }
}
