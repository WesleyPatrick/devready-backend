package br.com.devready.application.catalog.dto;

import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.ProblemType;
import java.util.Set;
import java.util.UUID;

public record ProblemSummary(
        UUID id,
        ProblemType type,
        String slug,
        String title,
        Difficulty difficulty,
        Level level,
        Set<String> tagSlugs) {
    public ProblemSummary {
        tagSlugs = Set.copyOf(tagSlugs);
    }
}
