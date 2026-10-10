package br.com.devready.application.career.fake;

import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.domain.career.CareerProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryCareerProfileRepository implements CareerProfileRepository {
    private final Map<UUID, CareerProfile> byUserId = new HashMap<>();

    @Override
    public Optional<CareerProfile> findByUserId(UUID userId) {
        return Optional.ofNullable(byUserId.get(userId));
    }

    @Override
    public CareerProfile save(CareerProfile profile) {
        byUserId.put(profile.userId(), profile);
        return profile;
    }
}
