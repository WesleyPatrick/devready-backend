package br.com.devready.application.catalog.fake;

import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryChallengeRepository implements ChallengeRepository {
    private final Map<String, Challenge> challenges = new HashMap<>();
    private int saves;

    @Override
    public boolean existsBySlug(String slug) {
        return challenges.containsKey(slug);
    }

    @Override
    public Optional<Challenge> findBySlug(String slug) {
        return Optional.ofNullable(challenges.get(slug));
    }

    @Override
    public Optional<Challenge> findById(UUID id) {
        return challenges.values().stream().filter(c -> c.id().equals(id)).findFirst();
    }

    @Override
    public Challenge save(Challenge challenge) {
        saves++;
        challenges.put(challenge.slug(), challenge);
        return challenge;
    }

    @Override
    public void updateDisplay(UUID id, String title, Difficulty difficulty, int maxSubmissions) {
        Challenge current =
                challenges.values().stream()
                        .filter(challenge -> challenge.id().equals(id))
                        .findFirst()
                        .orElseThrow();
        challenges.put(
                current.slug(),
                new Challenge(
                        current.id(),
                        current.slug(),
                        title,
                        current.areaId(),
                        current.level(),
                        difficulty,
                        current.statement(),
                        current.approachSummary(),
                        current.limits(),
                        current.comparisonMode(),
                        maxSubmissions,
                        current.languages(),
                        current.testCases(),
                        current.tagSlugs(),
                        current.active()));
    }

    @Override
    public long countActiveByAreaAndLevel(UUID areaId, Level level) {
        return challenges.values().stream()
                .filter(c -> c.active() && c.areaId().equals(areaId) && c.level() == level)
                .count();
    }

    public Challenge get(String slug) {
        return challenges.get(slug);
    }

    public Collection<Challenge> all() {
        return challenges.values();
    }

    public int saves() {
        return saves;
    }
}
