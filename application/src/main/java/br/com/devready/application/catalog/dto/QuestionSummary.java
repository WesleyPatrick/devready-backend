package br.com.devready.application.catalog.dto;

import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;

public record QuestionSummary(String slug, String title, Difficulty difficulty, Level level) {}
