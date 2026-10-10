package br.com.devready.application.career.dto;

import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.QuestionProgress;
import br.com.devready.domain.career.Stack;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record LearningTrackView(
        Stack stack,
        List<TrackCompetencyView> competencies,
        int jobsAnalyzed,
        List<Company> missingCompanies,
        Map<String, QuestionProgress> questionProgress,
        Level seniority) {

    public LearningTrackView {
        questionProgress = Map.copyOf(questionProgress);
    }

    public QuestionProgress progressOf(String questionSlug) {
        return questionProgress.getOrDefault(questionSlug, QuestionProgress.NOT_STARTED);
    }

    public Set<Level> visibleLevels() {
        return Level.upTo(seniority);
    }
}
