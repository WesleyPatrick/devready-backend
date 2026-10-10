package br.com.devready.application.career.usecase;

import br.com.devready.application.career.dto.OnboardingOptions;
import br.com.devready.application.career.port.CompanyRepository;
import br.com.devready.application.career.port.StackRepository;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.Stack;
import br.com.devready.domain.catalog.Level;
import java.util.Comparator;
import java.util.List;

public class ListOnboardingOptionsUseCase {

    private final CompanyRepository companies;
    private final StackRepository stacks;

    public ListOnboardingOptionsUseCase(CompanyRepository companies, StackRepository stacks) {
        this.companies = companies;
        this.stacks = stacks;
    }

    public OnboardingOptions execute() {
        List<Company> orderedCompanies =
                companies.findAllActive().stream()
                        .sorted(
                                Comparator.comparing(Company::group)
                                        .thenComparing(
                                                Company::name, String.CASE_INSENSITIVE_ORDER))
                        .toList();
        List<Stack> orderedStacks =
                stacks.findAll().stream()
                        .sorted(
                                Comparator.comparing(Stack::areaSlug)
                                        .thenComparing(Stack::name, String.CASE_INSENSITIVE_ORDER))
                        .toList();
        return new OnboardingOptions(orderedCompanies, orderedStacks, List.of(Level.values()));
    }
}
