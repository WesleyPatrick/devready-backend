package br.com.devready.application.career.fake;

import br.com.devready.application.career.port.TrackRepository;
import br.com.devready.domain.career.Track;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryTrackRepository implements TrackRepository {
    private final Map<UUID, Track> byId = new LinkedHashMap<>();

    @Override
    public List<Track> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public List<Track> findByCompanies(Collection<String> companySlugs) {
        return byId.values().stream().filter(t -> companySlugs.contains(t.companySlug())).toList();
    }

    @Override
    public Track save(Track track) {
        byId.put(track.id(), track);
        return track;
    }

    @Override
    public void delete(UUID id) {
        byId.remove(id);
    }

    public int count() {
        return byId.size();
    }
}
