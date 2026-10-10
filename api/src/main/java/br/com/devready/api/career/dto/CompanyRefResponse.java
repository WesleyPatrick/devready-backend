package br.com.devready.api.career.dto;

import br.com.devready.domain.career.Company;

public record CompanyRefResponse(String slug, String name) {
    public static CompanyRefResponse from(Company company) {
        return new CompanyRefResponse(company.slug(), company.name());
    }
}
