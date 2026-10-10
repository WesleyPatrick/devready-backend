package br.com.devready.infrastructure.career.persistence;

import br.com.devready.application.career.port.TrackRepository;
import br.com.devready.domain.career.Track;
import br.com.devready.domain.career.TrackUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaTrackRepository implements TrackRepository {

    private final SpringDataTrackRepository tracks;
    private final SpringDataCompanyRepository companies;
    private final SpringDataStackRepository stacks;
    private final SpringDataCompetencyRepository competencies;

    public JpaTrackRepository(
            SpringDataTrackRepository tracks,
            SpringDataCompanyRepository companies,
            SpringDataStackRepository stacks,
            SpringDataCompetencyRepository competencies) {
        this.tracks = tracks;
        this.companies = companies;
        this.stacks = stacks;
        this.competencies = competencies;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Track> findAll() {
        return toDomain(tracks.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Track> findByCompanies(Collection<String> companySlugs) {
        if (companySlugs.isEmpty()) {
            return List.of();
        }
        List<UUID> ids =
                companies.findBySlugIn(companySlugs).stream().map(CompanyEntity::getId).toList();
        return toDomain(tracks.findByCompanyIdIn(ids));
    }

    @Override
    public Track save(Track track) {
        TrackEntity entity =
                tracks.findById(track.id()).orElseGet(() -> new TrackEntity(track.id()));
        UUID companyId = companies.findBySlug(track.companySlug()).orElseThrow().getId();
        UUID stackId =
                track.stackSlug() == null
                        ? null
                        : stacks.findBySlug(track.stackSlug()).orElseThrow().getId();
        Map<String, UUID> competencyIds = new HashMap<>();
        competencies
                .findBySlugIn(track.units().stream().map(TrackUnit::competencySlug).toList())
                .forEach(c -> competencyIds.put(c.getSlug(), c.getId()));
        tracks.saveAndFlush(CareerMappers.apply(track, entity, companyId, stackId, competencyIds));
        return track;
    }

    @Override
    public void delete(UUID id) {
        tracks.deleteById(id);
        tracks.flush();
    }

    private List<Track> toDomain(List<TrackEntity> entities) {
        Map<UUID, String> companySlugs = new HashMap<>();
        companies.findAll().forEach(c -> companySlugs.put(c.getId(), c.getSlug()));
        Map<UUID, String> stackSlugs = new HashMap<>();
        stacks.findAll().forEach(s -> stackSlugs.put(s.getId(), s.getSlug()));
        Map<UUID, String> competencySlugs = new HashMap<>();
        competencies.findAll().forEach(c -> competencySlugs.put(c.getId(), c.getSlug()));
        return entities.stream()
                .map(e -> CareerMappers.toDomain(e, companySlugs, stackSlugs, competencySlugs))
                .toList();
    }
}
