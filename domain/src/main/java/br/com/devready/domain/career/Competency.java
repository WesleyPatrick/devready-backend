package br.com.devready.domain.career;

import br.com.devready.domain.catalog.Difficulty;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record Competency(
        UUID id,
        String slug,
        String name,
        CompetencyCategory category,
        String summary,
        Difficulty difficulty,
        Set<String> tagSlugs,
        List<Material> materials) {
    public Competency {
        if (id == null) {
            throw new CareerException("Competência sem id.");
        }
        Names.slug(slug, "competência");
        name = Names.text(name, "o nome da competência " + slug);
        if (category == null) {
            throw new CareerException("Competência " + slug + " sem categoria.");
        }
        if (difficulty == null) {
            throw new CareerException("Competência " + slug + " sem dificuldade.");
        }
        summary = summary == null ? "" : summary.strip();
        if (tagSlugs == null) {
            tagSlugs = Set.of();
        } else {
            for (String tag : tagSlugs) {
                Names.slug(tag, "tag da competência " + slug);
            }
            tagSlugs = Set.copyOf(tagSlugs);
        }
        if (materials == null) {
            materials = List.of();
        } else {
            for (Material material : materials) {
                if (material == null) {
                    throw new CareerException("Material vazio na competência " + slug + ".");
                }
            }
            materials = List.copyOf(materials);
        }
    }
}
