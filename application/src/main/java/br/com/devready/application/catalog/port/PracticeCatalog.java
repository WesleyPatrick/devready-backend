package br.com.devready.application.catalog.port;

import br.com.devready.application.catalog.dto.ChallengeSummary;
import br.com.devready.application.catalog.dto.ProblemSummary;
import br.com.devready.application.catalog.dto.QuestionSummary;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface PracticeCatalog {
    List<QuestionSummary> activeQuestions(UUID areaId, Set<String> tagSlugs, Set<Level> levels);

    List<ChallengeSummary> activeChallenges(UUID areaId, Set<String> tagSlugs, Set<Level> levels);

    List<ProblemSummary> activeProblems(UUID areaId, Set<Level> levels);
}
