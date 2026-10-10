package br.com.devready.application.catalog.dto;

import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import java.util.List;

public record QuestionSeed(
        String slug,
        String title,
        String areaSlug,
        Level level,
        Difficulty difficulty,
        String statement,
        String explanation,
        List<OptionSeed> options,
        List<String> tags) {
    public record OptionSeed(String text, boolean correct) {}
}
