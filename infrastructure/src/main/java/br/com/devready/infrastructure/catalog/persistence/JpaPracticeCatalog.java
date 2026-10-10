package br.com.devready.infrastructure.catalog.persistence;

import br.com.devready.application.catalog.dto.ChallengeSummary;
import br.com.devready.application.catalog.dto.ProblemSummary;
import br.com.devready.application.catalog.dto.QuestionSummary;
import br.com.devready.application.catalog.port.PracticeCatalog;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.ProblemType;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public class JpaPracticeCatalog implements PracticeCatalog {

    private final SpringDataQuestionRepository questions;
    private final SpringDataChallengeRepository challenges;

    public JpaPracticeCatalog(
            SpringDataQuestionRepository questions, SpringDataChallengeRepository challenges) {
        this.questions = questions;
        this.challenges = challenges;
    }

    @Override
    public List<QuestionSummary> activeQuestions(
            UUID areaId, Set<String> tagSlugs, Set<Level> levels) {
        if (tagSlugs.isEmpty() || levels.isEmpty()) {
            return List.of();
        }
        return questions.findActiveByAreaWithAnyTag(areaId, tagSlugs, levels).stream()
                .map(
                        q ->
                                new QuestionSummary(
                                        q.getSlug(), q.getTitle(), q.getDifficulty(), q.getLevel()))
                .toList();
    }

    @Override
    public List<ChallengeSummary> activeChallenges(
            UUID areaId, Set<String> tagSlugs, Set<Level> levels) {
        if (tagSlugs.isEmpty() || levels.isEmpty()) {
            return List.of();
        }
        return challenges.findActiveByAreaWithAnyTag(areaId, tagSlugs, levels).stream()
                .map(
                        c ->
                                new ChallengeSummary(
                                        c.getSlug(),
                                        c.getTitle(),
                                        c.getDifficulty(),
                                        c.getLevel(),
                                        c.getLanguages().stream()
                                                .map(ChallengeLanguageEntity::getLanguage)
                                                .sorted()
                                                .toList()))
                .toList();
    }

    @Override
    public List<ProblemSummary> activeProblems(UUID areaId, Set<Level> levels) {
        if (levels.isEmpty()) {
            return List.of();
        }
        List<ProblemSummary> problems = new ArrayList<>();
        for (QuestionEntity q : questions.findActiveByAreaWithTags(areaId, levels)) {
            problems.add(
                    new ProblemSummary(
                            q.getId(),
                            ProblemType.QUESTION,
                            q.getSlug(),
                            q.getTitle(),
                            q.getDifficulty(),
                            q.getLevel(),
                            CatalogMappers.tagSlugs(q.getTags())));
        }
        for (ChallengeEntity c : challenges.findActiveByAreaWithTags(areaId, levels)) {
            problems.add(
                    new ProblemSummary(
                            c.getId(),
                            ProblemType.CHALLENGE,
                            c.getSlug(),
                            c.getTitle(),
                            c.getDifficulty(),
                            c.getLevel(),
                            CatalogMappers.tagSlugs(c.getTags())));
        }
        return problems;
    }
}
