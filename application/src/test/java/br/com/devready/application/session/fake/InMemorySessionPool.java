package br.com.devready.application.session.fake;

import br.com.devready.application.catalog.fake.InMemoryAreaRepository;
import br.com.devready.application.catalog.fake.InMemoryChallengeRepository;
import br.com.devready.application.catalog.fake.InMemoryQuestionRepository;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class InMemorySessionPool implements SessionPool {

    private final InMemoryAreaRepository areas;
    private final InMemoryQuestionRepository questions;
    private final InMemoryChallengeRepository challenges;

    public InMemorySessionPool(
            InMemoryAreaRepository areas,
            InMemoryQuestionRepository questions,
            InMemoryChallengeRepository challenges) {
        this.areas = areas;
        this.questions = questions;
        this.challenges = challenges;
    }

    @Override
    public Optional<Area> area(UUID areaId) {
        return areas.findAllActive().stream().filter(a -> a.id().equals(areaId)).findFirst();
    }

    @Override
    public List<Question> activeQuestions(UUID areaId, Level level) {
        return questions.all().stream()
                .filter(q -> q.active() && q.areaId().equals(areaId) && q.level() == level)
                .toList();
    }

    @Override
    public List<Challenge> activeChallenges(UUID areaId, Level level) {
        return challenges.all().stream()
                .filter(c -> c.active() && c.areaId().equals(areaId) && c.level() == level)
                .toList();
    }

    @Override
    public List<Question> activeQuestionsTagged(
            UUID areaId, Set<String> tagSlugs, Set<Level> levels) {
        return questions.all().stream()
                .filter(q -> q.active() && q.areaId().equals(areaId) && levels.contains(q.level()))
                .filter(q -> q.tagSlugs().stream().anyMatch(tagSlugs::contains))
                .toList();
    }

    @Override
    public List<Question> questions(Collection<UUID> ids) {
        return questions.all().stream().filter(q -> ids.contains(q.id())).toList();
    }

    @Override
    public List<Challenge> challenges(Collection<UUID> ids) {
        return challenges.all().stream().filter(c -> ids.contains(c.id())).toList();
    }
}
