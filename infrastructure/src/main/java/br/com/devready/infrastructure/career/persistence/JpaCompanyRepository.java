package br.com.devready.infrastructure.career.persistence;

import br.com.devready.application.career.port.CompanyRepository;
import br.com.devready.domain.career.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaCompanyRepository implements CompanyRepository {

    private final SpringDataCompanyRepository companies;

    public JpaCompanyRepository(SpringDataCompanyRepository companies) {
        this.companies = companies;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Company> findAllActive() {
        return companies.findByActiveTrue().stream().map(CareerMappers::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Company> findBySlug(String slug) {
        return companies.findBySlug(slug).map(CareerMappers::toDomain);
    }

    @Override
    public Company save(Company company) {
        CompanyEntity entity =
                companies.findById(company.id()).orElseGet(() -> new CompanyEntity(company.id()));
        return CareerMappers.toDomain(companies.saveAndFlush(CareerMappers.apply(company, entity)));
    }
}
