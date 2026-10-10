package br.com.devready.infrastructure.career.persistence;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataTrackRepository extends JpaRepository<TrackEntity, UUID> {
    List<TrackEntity> findByCompanyIdIn(Collection<UUID> companyIds);
}
