package br.com.devready.application.session.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.career.port.CompetencyRepository;
import br.com.devready.application.evaluation.fake.FixedLanguageRuntimes;
import br.com.devready.application.session.dto.SessionSummaryView;
import br.com.devready.application.session.fake.SessionWorld;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.application.session.service.SessionViews;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.session.Session;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListSessionsUseCaseTest {

    private final SessionWorld world = new SessionWorld();
    private final ListSessionsUseCase list =
            new ListSessionsUseCase(
                    world.sessions,
                    new SessionViews(
                            world.pool,
                            world.practice.submissions,
                            world.practice.competencies,
                            new FixedLanguageRuntimes()));

    @Test
    void newestFirstWithTheAreaAndTheCompetencyOfTheLesson() {
        Session lesson = world.openLesson();
        world.finishSession.execute(world.user, lesson.id());
        world.clock.advanceTo(world.clock.now().plusSeconds(60));
        Session simulation = world.openSimulation();

        var items = list.execute(world.user);

        assertThat(items)
                .extracting(item -> item.session().id())
                .containsExactly(simulation.id(), lesson.id());
        SessionSummaryView first = items.get(1);
        assertThat(first.area().slug()).isEqualTo("backend");
        assertThat(first.competency().name()).isEqualTo("SQL");
        assertThat(items.get(0).competency()).isNull();
        assertThat(list.execute(UUID.randomUUID())).isEmpty();
    }

    @Test
    void readsTheAreasAndTheCompetenciesOnceForTheWholeHistory() {
        CountingPool pool = new CountingPool(world.pool);
        CountingCompetencies competencies = new CountingCompetencies(world.practice.competencies);
        ListSessionsUseCase counted =
                new ListSessionsUseCase(
                        world.sessions,
                        new SessionViews(
                                pool,
                                world.practice.submissions,
                                competencies,
                                new FixedLanguageRuntimes()));
        for (int i = 0; i < 4; i++) {
            Session lesson = world.openLesson();
            world.finishSession.execute(world.user, lesson.id());
            world.clock.advanceTo(world.clock.now().plusSeconds(60));
        }

        assertThat(counted.execute(world.user)).hasSize(4);
        assertThat(pool.areaReads).isEqualTo(1);
        assertThat(competencies.reads).isEqualTo(1);
    }

    private static final class CountingPool implements SessionPool {
        private final SessionPool pool;
        private int areaReads;

        CountingPool(SessionPool pool) {
            this.pool = pool;
        }

        @Override
        public Optional<Area> area(UUID areaId) {
            areaReads++;
            return pool.area(areaId);
        }

        @Override
        public List<Question> activeQuestions(UUID areaId, Level level) {
            return pool.activeQuestions(areaId, level);
        }

        @Override
        public List<Challenge> activeChallenges(UUID areaId, Level level) {
            return pool.activeChallenges(areaId, level);
        }

        @Override
        public List<Question> activeQuestionsTagged(
                UUID areaId, Set<String> tagSlugs, Set<Level> levels) {
            return pool.activeQuestionsTagged(areaId, tagSlugs, levels);
        }

        @Override
        public List<Question> questions(Collection<UUID> ids) {
            return pool.questions(ids);
        }

        @Override
        public List<Challenge> challenges(Collection<UUID> ids) {
            return pool.challenges(ids);
        }
    }

    private static final class CountingCompetencies implements CompetencyRepository {
        private final CompetencyRepository competencies;
        private int reads;

        CountingCompetencies(CompetencyRepository competencies) {
            this.competencies = competencies;
        }

        @Override
        public List<Competency> findAll() {
            reads++;
            return competencies.findAll();
        }

        @Override
        public Competency save(Competency competency) {
            return competencies.save(competency);
        }

        @Override
        public void delete(String slug) {
            competencies.delete(slug);
        }
    }
}
