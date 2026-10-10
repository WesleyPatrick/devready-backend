package br.com.devready.application.catalog.fake;

import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.domain.catalog.Area;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InMemoryAreaRepository implements AreaRepository {
    private final Map<String, Area> areas = new LinkedHashMap<>();

    @Override
    public List<Area> findAllActive() {
        return areas.values().stream().filter(Area::active).toList();
    }

    @Override
    public Optional<Area> findBySlug(String slug) {
        return Optional.ofNullable(areas.get(slug));
    }

    @Override
    public Area save(Area area) {
        areas.put(area.slug(), area);
        return area;
    }
}
