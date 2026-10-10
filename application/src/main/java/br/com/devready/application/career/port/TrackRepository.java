package br.com.devready.application.career.port;

import br.com.devready.domain.career.Track;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface TrackRepository {
    List<Track> findAll();

    List<Track> findByCompanies(Collection<String> companySlugs);

    Track save(Track track);

    void delete(UUID id);
}
