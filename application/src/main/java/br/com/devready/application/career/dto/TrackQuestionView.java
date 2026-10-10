package br.com.devready.application.career.dto;

import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;

public record TrackQuestionView(
        String slug, String title, Difficulty difficulty, Level level, QuestionProgress progress) {}
