package br.com.devready.infrastructure.catalog.persistence;

import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.domain.catalog.Area;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaAreaRepository implements AreaRepository {

    private final SpringDataAreaRepository areas;

    public JpaAreaRepository(SpringDataAreaRepository areas) {
        this.areas = areas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Area> findAllActive() {
        return areas.findByActiveTrueOrderByNameAsc().stream()
                .map(CatalogMappers::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Area> findBySlug(String slug) {
        return areas.findBySlug(slug).map(CatalogMappers::toDomain);
    }

    @Override
    public Area save(Area area) {
        AreaEntity entity = areas.findById(area.id()).orElseGet(() -> new AreaEntity(area.id()));
        return CatalogMappers.toDomain(areas.save(CatalogMappers.apply(area, entity)));
    }
}
