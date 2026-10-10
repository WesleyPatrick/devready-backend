package br.com.devready.application.career.port;

import br.com.devready.domain.career.Competency;
import java.util.List;

public interface CompetencyRepository {
    List<Competency> findAll();

    Competency save(Competency competency);

    void delete(String slug);
}
