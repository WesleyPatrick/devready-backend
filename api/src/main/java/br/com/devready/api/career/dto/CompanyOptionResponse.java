package br.com.devready.api.career.dto;

import br.com.devready.domain.career.Company;

public record CompanyOptionResponse(String slug, String name, String group) {
    public static CompanyOptionResponse from(Company company) {
        return new CompanyOptionResponse(company.slug(), company.name(), company.group().slug());
    }
}
