package br.com.devready.application.catalog.dto;

import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import java.util.List;

public record ChallengeSummary(
        String slug, String title, Difficulty difficulty, Level level, List<Language> languages) {}
