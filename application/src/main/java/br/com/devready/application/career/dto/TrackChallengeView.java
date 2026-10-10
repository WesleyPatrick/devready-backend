package br.com.devready.application.career.dto;

import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import java.util.List;

public record TrackChallengeView(
        String slug,
        String title,
        Difficulty difficulty,
        Level level,
        List<Language> languages,
        QuestionProgress progress) {
    public TrackChallengeView {
        languages = List.copyOf(languages);
    }
}
