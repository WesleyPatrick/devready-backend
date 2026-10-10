package br.com.devready.application.career.port;

import br.com.devready.domain.career.CareerProfile;
import java.util.Optional;
import java.util.UUID;

public interface CareerProfileRepository {
    Optional<CareerProfile> findByUserId(UUID userId);

    CareerProfile save(CareerProfile profile);
}
