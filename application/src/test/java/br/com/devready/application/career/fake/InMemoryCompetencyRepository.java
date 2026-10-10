package br.com.devready.application.career.fake;

import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.domain.career.Competency;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryCompetencyRepository implements CompetencyRepository {
    private final Map<String, Competency> bySlug = new LinkedHashMap<>();

    @Override
    public List<Competency> findAll() {
        return new ArrayList<>(bySlug.values());
    }

    @Override
    public Competency save(Competency competency) {
        bySlug.put(competency.slug(), competency);
        return competency;
    }

    @Override
    public void delete(String slug) {
        bySlug.remove(slug);
    }

    public Optional<Competency> find(String slug) {
        return Optional.ofNullable(bySlug.get(slug));
    }

    public int count() {
        return bySlug.size();
    }
}
