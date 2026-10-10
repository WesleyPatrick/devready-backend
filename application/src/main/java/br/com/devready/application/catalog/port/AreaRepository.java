package br.com.devready.application.catalog.port;

import br.com.devready.domain.catalog.Area;
import java.util.List;
import java.util.Optional;

public interface AreaRepository {
    List<Area> findAllActive();

    Optional<Area> findBySlug(String slug);

    Area save(Area area);
}
