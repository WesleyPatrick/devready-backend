package br.com.devready.application.career.usecase;

import static br.com.devready.application.career.fake.CareerFixtures.company;
import static br.com.devready.application.career.fake.CareerFixtures.stack;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.career.fake.InMemoryCompanyRepository;
import br.com.devready.application.career.fake.InMemoryStackRepository;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.career.Stack;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListOnboardingOptionsUseCaseTest {

    @Test
    void ordersCompaniesByGroupThenNameIgnoringCaseAndHidesInactive() {
        InMemoryCompanyRepository companies = new InMemoryCompanyRepository();
        companies.save(company("zoom", "Zoom", CompanyGroup.BIG_TECH));
        companies.save(company("ame", "Ame", CompanyGroup.TECH_BRASIL));
        companies.save(company("google", "Google", CompanyGroup.BIG_TECH));
        companies.save(company("apple", "apple", CompanyGroup.BIG_TECH));
        companies.save(company("amazon", "Amazon", CompanyGroup.BIG_TECH));
        companies.save(
                new Company(
                        UUID.randomUUID(),
                        "velha",
                        "Velha",
                        CompanyGroup.BIG_TECH,
                        "",
                        List.of(),
                        false));
        InMemoryStackRepository stacks = new InMemoryStackRepository();

        var options = new ListOnboardingOptionsUseCase(companies, stacks).execute();

        assertThat(options.companies())
                .extracting(Company::slug)
                .containsExactly("amazon", "apple", "google", "zoom", "ame");
    }

    @Test
    void ordersStacksByAreaThenNameIgnoringCase() {
        InMemoryCompanyRepository companies = new InMemoryCompanyRepository();
        InMemoryStackRepository stacks = new InMemoryStackRepository();
        stacks.save(stack("zzz", "Zzz", "backend"));
        stacks.save(stack("aaa", "Aaa", "frontend"));
        stacks.save(stack("react", "React", "frontend"));
        stacks.save(stack("java-spring", "Java e Spring", "backend"));
        stacks.save(stack("dotnet", "dotNET", "backend"));

        var options = new ListOnboardingOptionsUseCase(companies, stacks).execute();

        assertThat(options.stacks())
                .extracting(Stack::slug)
                .containsExactly("dotnet", "java-spring", "zzz", "aaa", "react");
    }

    @Test
    void offersAllSenioritiesInOrder() {
        var options =
                new ListOnboardingOptionsUseCase(
                                new InMemoryCompanyRepository(), new InMemoryStackRepository())
                        .execute();

        assertThat(options.seniorities()).containsExactly(Level.JUNIOR, Level.PLENO, Level.SENIOR);
    }
}
