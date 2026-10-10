package br.com.devready.application.career.usecase;

import br.com.devready.application.career.dto.CareerProfileView;
import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.career.exception.UnknownStackException;
import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.application.career.port.CompanyRepository;
import br.com.devready.application.career.port.StackRepository;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.Stack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class GetCareerProfileUseCase {

    private final CareerProfileRepository profiles;
    private final CompanyRepository companies;
    private final StackRepository stacks;

    public GetCareerProfileUseCase(
            CareerProfileRepository profiles, CompanyRepository companies, StackRepository stacks) {
        this.profiles = profiles;
        this.companies = companies;
        this.stacks = stacks;
    }

    public CareerProfileView execute(UUID userId) {
        CareerProfile profile =
                profiles.findByUserId(userId).orElseThrow(CareerProfileNotFoundException::new);
        List<Company> chosen = new ArrayList<>();
        for (String slug : profile.companySlugs()) {
            companies.findBySlug(slug).ifPresent(chosen::add);
        }
        Stack stack =
                stacks.findBySlug(profile.stackSlug())
                        .orElseThrow(() -> new UnknownStackException(profile.stackSlug()));
        return new CareerProfileView(chosen, stack, profile.seniority());
    }
}
