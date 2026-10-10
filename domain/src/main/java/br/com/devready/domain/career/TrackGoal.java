package br.com.devready.domain.career;

import br.com.devready.domain.catalog.Level;
import java.util.List;

public record TrackGoal(
        List<String> companySlugs, String areaSlug, String stackSlug, Level seniority) {
    public TrackGoal {
        if (companySlugs == null) {
            companySlugs = List.of();
        } else {
            for (String company : companySlugs) {
                Names.slug(company, "empresa do objetivo");
            }
            companySlugs = List.copyOf(companySlugs);
        }
        Names.slug(areaSlug, "área do objetivo");
        Names.slug(stackSlug, "stack do objetivo");
        if (seniority == null) {
            throw new CareerException("Objetivo sem senioridade.");
        }
    }
}
