package br.com.devready.application.catalog.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.catalog.dto.AreaView;
import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.catalog.fake.InMemoryAreaRepository;
import br.com.devready.application.catalog.fake.InMemoryChallengeRepository;
import br.com.devready.application.catalog.fake.InMemoryQuestionRepository;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.SessionConfig;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListAreasUseCaseTest {

    private final InMemoryAreaRepository areas = new InMemoryAreaRepository();
    private final InMemoryQuestionRepository questions = new InMemoryQuestionRepository();
    private final InMemoryChallengeRepository challenges = new InMemoryChallengeRepository();
    private final ListAreasUseCase list = new ListAreasUseCase(areas, questions, challenges);

    private Area area(String slug, boolean active, int questionCount, int challengeCount) {
        return areas.save(
                new Area(
                        UUID.randomUUID(),
                        slug,
                        slug,
                        new SessionConfig(questionCount, challengeCount, 45, 0.5),
                        active));
    }

    @Test
    void returnsOnlyActiveAreasInInsertionOrder() {
        area("backend", true, 1, 1);
        area("mobile", false, 1, 1);
        area("devops", true, 1, 1);

        assertThat(list.execute())
                .extracting(view -> view.area().slug())
                .containsExactly("backend", "devops");
    }

    @Test
    void aLevelAppearsOnlyWithEnoughActiveQuestionsAndChallenges() {
        Area backend = area("backend", true, 2, 1);
        questions.save(CatalogFixtures.question("q1", "Um", backend, Difficulty.EASY, true, "t"));
        questions.save(CatalogFixtures.question("q2", "Dois", backend, Difficulty.EASY, true, "t"));
        questions.save(
                CatalogFixtures.question("q3", "Inativa", backend, Difficulty.EASY, false, "t"));
        challenges.save(CatalogFixtures.challenge("c1", "Um", backend, Difficulty.EASY, "t"));
        Area devops = area("devops", true, 3, 1);
        questions.save(
                CatalogFixtures.question("q4", "Só uma", devops, Difficulty.EASY, true, "t"));
        challenges.save(CatalogFixtures.challenge("c2", "Um", devops, Difficulty.EASY, "t"));

        AreaView[] views = list.execute().toArray(AreaView[]::new);

        assertThat(views[0].levels()).containsExactly(Level.JUNIOR);
        assertThat(views[1].levels()).isEmpty();
    }
}
