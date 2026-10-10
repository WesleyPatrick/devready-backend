package br.com.devready.infrastructure.career.persistence;

import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.catalog.Level;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional
public class JpaCareerProfileRepository implements CareerProfileRepository {

    private final SpringDataCareerProfileRepository profiles;
    private final SpringDataCompanyRepository companies;
    private final SpringDataStackRepository stacks;

    public JpaCareerProfileRepository(
            SpringDataCareerProfileRepository profiles,
            SpringDataCompanyRepository companies,
            SpringDataStackRepository stacks) {
        this.profiles = profiles;
        this.companies = companies;
        this.stacks = stacks;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CareerProfile> findByUserId(UUID userId) {
        return profiles.findById(userId)
                .map(
                        entity ->
                                new CareerProfile(
                                        entity.getUserId(),
                                        companySlugsInOrder(entity.getCompanyIds()),
                                        stacks.findById(entity.getStackId())
                                                .orElseThrow()
                                                .getSlug(),
                                        Level.valueOf(entity.getSeniority())));
    }

    private List<String> companySlugsInOrder(List<UUID> ids) {
        Map<UUID, String> slugById = new HashMap<>();
        companies.findAllById(ids).forEach(c -> slugById.put(c.getId(), c.getSlug()));
        return ids.stream().map(id -> Optional.ofNullable(slugById.get(id)).orElseThrow()).toList();
    }

    @Override
    public CareerProfile save(CareerProfile profile) {
        CareerProfileEntity entity =
                profiles.findById(profile.userId())
                        .orElseGet(() -> new CareerProfileEntity(profile.userId()));
        entity.setStackId(stacks.findBySlug(profile.stackSlug()).orElseThrow().getId());
        entity.setSeniority(profile.seniority().name());
        entity.setCompanyIds(
                profile.companySlugs().stream()
                        .map(slug -> companies.findBySlug(slug).orElseThrow().getId())
                        .toList());
        entity.touch();
        profiles.saveAndFlush(entity);
        return profile;
    }
}
