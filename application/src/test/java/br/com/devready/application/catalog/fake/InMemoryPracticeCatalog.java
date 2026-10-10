package br.com.devready.application.catalog.fake;

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

public class InMemoryPracticeCatalog implements PracticeCatalog {

    private final InMemoryQuestionRepository questions;
    private final InMemoryChallengeRepository challenges;

    public InMemoryPracticeCatalog(
            InMemoryQuestionRepository questions, InMemoryChallengeRepository challenges) {
        this.questions = questions;
        this.challenges = challenges;
    }

    @Override
    public List<QuestionSummary> activeQuestions(
            UUID areaId, Set<String> tagSlugs, Set<Level> levels) {
        return questions.all().stream()
                .filter(q -> q.active() && q.areaId().equals(areaId) && levels.contains(q.level()))
                .filter(q -> q.tagSlugs().stream().anyMatch(tagSlugs::contains))
                .map(q -> new QuestionSummary(q.slug(), q.title(), q.difficulty(), q.level()))
                .toList();
    }

    @Override
    public List<ChallengeSummary> activeChallenges(
            UUID areaId, Set<String> tagSlugs, Set<Level> levels) {
        return challenges.all().stream()
                .filter(c -> c.active() && c.areaId().equals(areaId) && levels.contains(c.level()))
                .filter(c -> c.tagSlugs().stream().anyMatch(tagSlugs::contains))
                .map(
                        c ->
                                new ChallengeSummary(
                                        c.slug(),
                                        c.title(),
                                        c.difficulty(),
                                        c.level(),
                                        List.copyOf(c.languages().keySet())))
                .toList();
    }

    @Override
    public List<ProblemSummary> activeProblems(UUID areaId, Set<Level> levels) {
        List<ProblemSummary> problems = new ArrayList<>();
        questions.all().stream()
                .filter(q -> q.active() && q.areaId().equals(areaId) && levels.contains(q.level()))
                .forEach(
                        q ->
                                problems.add(
                                        new ProblemSummary(
                                                q.id(),
                                                ProblemType.QUESTION,
                                                q.slug(),
                                                q.title(),
                                                q.difficulty(),
                                                q.level(),
                                                q.tagSlugs())));
        challenges.all().stream()
                .filter(c -> c.active() && c.areaId().equals(areaId) && levels.contains(c.level()))
                .forEach(
                        c ->
                                problems.add(
                                        new ProblemSummary(
                                                c.id(),
                                                ProblemType.CHALLENGE,
                                                c.slug(),
                                                c.title(),
                                                c.difficulty(),
                                                c.level(),
                                                c.tagSlugs())));
        return problems;
    }
}
