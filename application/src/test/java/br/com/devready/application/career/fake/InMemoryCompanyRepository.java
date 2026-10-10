package br.com.devready.application.career.fake;

import br.com.devready.application.career.port.CompanyRepository;
import br.com.devready.domain.career.Company;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCompanyRepository implements CompanyRepository {
    private final Map<String, Company> bySlug = new LinkedHashMap<>();

    @Override
    public List<Company> findAllActive() {
        return new ArrayList<>(bySlug.values().stream().filter(Company::active).toList());
    }

    @Override
    public Optional<Company> findBySlug(String slug) {
        return Optional.ofNullable(bySlug.get(slug));
    }

    @Override
    public Company save(Company company) {
        bySlug.put(company.slug(), company);
        return company;
    }

    public void remove(String slug) {
        bySlug.remove(slug);
    }

    public int count() {
        return bySlug.size();
    }
}
