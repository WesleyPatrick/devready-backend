package br.com.devready.application.career.port;

import br.com.devready.domain.career.Company;
import java.util.List;
import java.util.Optional;

public interface CompanyRepository {
    List<Company> findAllActive();

    Optional<Company> findBySlug(String slug);

    Company save(Company company);
}
