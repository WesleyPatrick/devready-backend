package br.com.devready.application.career.dto;

import java.util.List;

public record CompetencySeed(
        String slug,
        String name,
        String category,
        String summary,
        String difficulty,
        List<String> tags) {
    public CompetencySeed {
        tags = tags == null ? List.of() : tags;
    }
}
