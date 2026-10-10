package br.com.devready.api.career.dto;

import br.com.devready.application.career.dto.OnboardingOptions;
import br.com.devready.domain.catalog.Level;
import java.util.List;

public record OnboardingOptionsResponse(
        List<CompanyOptionResponse> companies,
        List<StackOptionResponse> stacks,
        List<String> seniorities) {
    public static OnboardingOptionsResponse from(OnboardingOptions options) {
        return new OnboardingOptionsResponse(
                options.companies().stream().map(CompanyOptionResponse::from).toList(),
                options.stacks().stream().map(StackOptionResponse::from).toList(),
                options.seniorities().stream().map(Level::name).toList());
    }
}
