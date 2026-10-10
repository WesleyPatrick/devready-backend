package br.com.devready.application.career.usecase;

import br.com.devready.application.career.dto.CareerProfileView;
import br.com.devready.application.career.dto.SaveCareerProfileCommand;
import br.com.devready.application.career.exception.UnknownCompanyException;
import br.com.devready.application.career.exception.UnknownStackException;
import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.application.career.port.CompanyRepository;
import br.com.devready.application.career.port.StackRepository;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.Stack;
import java.util.ArrayList;
import java.util.List;

public class SaveCareerProfileUseCase {

    private final CareerProfileRepository profiles;
    private final CompanyRepository companies;
    private final StackRepository stacks;

    public SaveCareerProfileUseCase(
            CareerProfileRepository profiles, CompanyRepository companies, StackRepository stacks) {
        this.profiles = profiles;
        this.companies = companies;
        this.stacks = stacks;
    }

    public CareerProfileView execute(SaveCareerProfileCommand command) {
        CareerProfile profile =
                new CareerProfile(
                        command.userId(),
                        command.companies(),
                        command.stack(),
                        command.seniority());
        List<Company> chosen = new ArrayList<>();
        List<String> unknown = new ArrayList<>();
        for (String slug : profile.companySlugs()) {
            companies
                    .findBySlug(slug)
                    .filter(Company::active)
                    .ifPresentOrElse(chosen::add, () -> unknown.add(slug));
        }
        if (!unknown.isEmpty()) {
            throw new UnknownCompanyException(unknown);
        }
        Stack stack =
                stacks.findBySlug(profile.stackSlug())
                        .orElseThrow(() -> new UnknownStackException(profile.stackSlug()));
        profiles.save(profile);
        return new CareerProfileView(chosen, stack, profile.seniority());
    }
}
