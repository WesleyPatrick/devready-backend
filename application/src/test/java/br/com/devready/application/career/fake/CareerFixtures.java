package br.com.devready.application.career.fake;

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
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CareerFixtures {
    private CareerFixtures() {}

    public static Company company(String slug, String name, CompanyGroup group) {
        return new Company(UUID.randomUUID(), slug, name, group, "", List.of(), true);
    }

    public static Stack stack(String slug, String name, String area) {
        return new Stack(UUID.randomUUID(), slug, name, area);
    }

    public static Competency competency(
            String slug,
            String name,
            CompetencyCategory category,
            Difficulty difficulty,
            String... tags) {
        return new Competency(
                UUID.randomUUID(),
                slug,
                name,
                category,
                "Resumo de " + name + ".",
                difficulty,
                Set.of(tags),
                List.of(
                        new Material(
                                "Material de " + name,
                                "https://exemplo.com/" + slug,
                                MaterialType.DOCUMENTACAO,
                                "pt",
                                true)));
    }

    public static TrackUnit unit(
            String competency, double weight, Requirement requirement, int jobs) {
        return new TrackUnit(competency, weight, requirement, jobs);
    }

    public static Track track(
            String company,
            String area,
            String stack,
            Level seniority,
            int jobs,
            TrackUnit... units) {
        return new Track(UUID.randomUUID(), company, area, stack, seniority, jobs, List.of(units));
    }
}
