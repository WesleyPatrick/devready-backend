package br.com.devready.application.practice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.fake.InMemoryCareerProfileRepository;
import br.com.devready.application.catalog.fake.CatalogFixtures;
import br.com.devready.application.practice.exception.ChallengeNotFoundException;
import br.com.devready.application.practice.exception.QuestionNotFoundException;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VisibleLevelsTest {

    private final InMemoryCareerProfileRepository profiles = new InMemoryCareerProfileRepository();
    private final VisibleLevels levels = new VisibleLevels(profiles);
    private final UUID user = UUID.randomUUID();
    private final Area backend = CatalogFixtures.area("backend");

    private void goal(Level seniority) {
        profiles.save(new CareerProfile(user, List.of("nubank"), "java-spring", seniority));
    }

    @Test
    void withoutAGoalOnlyJuniorIsVisible() {
        assertThat(levels.of(user)).containsExactly(Level.JUNIOR);
    }

    @Test
    void theSeniorityOfTheGoalDecidesTheVisibleLevels() {
        goal(Level.PLENO);
        assertThat(levels.of(user)).containsExactly(Level.JUNIOR, Level.PLENO);

        goal(Level.SENIOR);
        assertThat(levels.of(user)).containsExactly(Level.JUNIOR, Level.PLENO, Level.SENIOR);
        assertThat(levels.of(UUID.randomUUID())).containsExactly(Level.JUNIOR);
    }

    @Test
    void aQuestionAboveTheGoalIsNotFoundLikeOneThatDoesNotExist() {
        goal(Level.PLENO);
        Question pleno =
                CatalogFixtures.atLevel(
                        CatalogFixtures.question(
                                "q-p", "Pleno", backend, Difficulty.EASY, true, "sql"),
                        Level.PLENO);
        Question senior =
                CatalogFixtures.atLevel(
                        CatalogFixtures.question(
                                "q-s", "Sênior", backend, Difficulty.EASY, true, "sql"),
                        Level.SENIOR);

        assertThat(levels.question(user, pleno)).isSameAs(pleno);
        assertThatThrownBy(() -> levels.question(user, senior))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessage("Questão não encontrada: q-s.");
    }

    @Test
    void aChallengeAboveTheGoalIsNotFoundLikeOneThatDoesNotExist() {
        goal(Level.JUNIOR);
        Challenge junior = CatalogFixtures.challenge("c-j", "Júnior", backend, Difficulty.EASY);
        Challenge pleno =
                CatalogFixtures.atLevel(
                        CatalogFixtures.challenge("c-p", "Pleno", backend, Difficulty.EASY),
                        Level.PLENO);

        assertThat(levels.challenge(user, junior)).isSameAs(junior);
        assertThatThrownBy(() -> levels.challenge(user, pleno))
                .isInstanceOf(ChallengeNotFoundException.class)
                .hasMessage("Desafio não encontrado: c-p.");
    }
}
