package br.com.devready.infrastructure.career.persistence;

import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.CompanyGroup;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.career.CompetencyCategory;
import br.com.devready.domain.career.Material;
import br.com.devready.domain.career.MaterialType;
import br.com.devready.domain.career.Requirement;
import br.com.devready.domain.career.Stack;
import br.com.devready.domain.career.Track;
import br.com.devready.domain.career.TrackUnit;
import br.com.devready.domain.catalog.Level;
import br.com.devready.infrastructure.catalog.persistence.TagEntity;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class CareerMappers {

    private CareerMappers() {}

    static Company toDomain(CompanyEntity e) {
        return new Company(
                e.getId(),
                e.getSlug(),
                e.getName(),
                CompanyGroup.fromSlug(e.getGroup()),
                e.getAbout(),
                e.getCareers(),
                e.isActive());
    }

    static CompanyEntity apply(Company company, CompanyEntity e) {
        e.setSlug(company.slug());
        e.setName(company.name());
        e.setGroup(company.group().slug());
        e.setAbout(company.about());
        e.setActive(company.active());
        e.setCareers(company.careers());
        return e;
    }

    static Stack toDomain(StackEntity e) {
        return new Stack(e.getId(), e.getSlug(), e.getName(), e.getAreaSlug());
    }

    static StackEntity apply(Stack stack, StackEntity e) {
        e.setSlug(stack.slug());
        e.setName(stack.name());
        e.setAreaSlug(stack.areaSlug());
        return e;
    }

    static Competency toDomain(CompetencyEntity e) {
        return new Competency(
                e.getId(),
                e.getSlug(),
                e.getName(),
                CompetencyCategory.fromSlug(e.getCategory()),
                e.getSummary(),
                e.getDifficulty(),
                e.getTags().stream().map(TagEntity::getSlug).collect(Collectors.toSet()),
                e.getMaterials().stream()
                        .map(
                                m ->
                                        new Material(
                                                m.getTitle(),
                                                m.getUrl(),
                                                MaterialType.fromSlug(m.getType()),
                                                m.getLanguage(),
                                                m.isFree()))
                        .toList());
    }

    static CompetencyEntity apply(Competency c, CompetencyEntity e, Set<TagEntity> tags) {
        e.setSlug(c.slug());
        e.setName(c.name());
        e.setCategory(c.category().slug());
        e.setSummary(c.summary());
        e.setDifficulty(c.difficulty());
        e.getTags().clear();
        e.getTags().addAll(tags);
        e.setMaterials(
                c.materials().stream()
                        .map(
                                m ->
                                        new MaterialEmbeddable(
                                                m.title(),
                                                m.url(),
                                                m.type().slug(),
                                                m.language(),
                                                m.free()))
                        .toList());
        return e;
    }

    static Track toDomain(
            TrackEntity e,
            Map<UUID, String> companySlugs,
            Map<UUID, String> stackSlugs,
            Map<UUID, String> competencySlugs) {
        List<TrackUnit> units =
                e.getUnits().stream()
                        .map(
                                u ->
                                        new TrackUnit(
                                                competencySlugs.get(u.getCompetencyId()),
                                                u.getWeight(),
                                                Requirement.fromSlug(u.getRequirement()),
                                                u.getJobs()))
                        .toList();
        return new Track(
                e.getId(),
                companySlugs.get(e.getCompanyId()),
                e.getAreaSlug(),
                e.getStackId() == null ? null : stackSlugs.get(e.getStackId()),
                e.getSeniority() == null ? null : Level.valueOf(e.getSeniority()),
                e.getJobs(),
                units);
    }

    static TrackEntity apply(
            Track t, TrackEntity e, UUID companyId, UUID stackId, Map<String, UUID> competencyIds) {
        e.setCompanyId(companyId);
        e.setAreaSlug(t.areaSlug());
        e.setStackId(stackId);
        e.setSeniority(t.seniority() == null ? null : t.seniority().name());
        e.setJobs(t.jobs());
        e.setSample(false);
        e.setUnits(
                t.units().stream()
                        .map(
                                u ->
                                        new TrackUnitEmbeddable(
                                                competencyIds.get(u.competencySlug()),
                                                u.weight(),
                                                u.requirement().slug(),
                                                u.jobs()))
                        .toList());
        return e;
    }
}
