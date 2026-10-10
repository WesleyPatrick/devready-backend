package br.com.devready.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class QuestionTest {

    private static QuestionOption option(String text, boolean correct, int position) {
        return new QuestionOption(UUID.randomUUID(), text, correct, position);
    }

    private static Question question(List<QuestionOption> options) {
        return new Question(
                UUID.randomUUID(),
                "backend-junior-http-idempotency",
                "Idempotência dos métodos HTTP",
                UUID.randomUUID(),
                Level.JUNIOR,
                Difficulty.EASY,
                "Qual método HTTP é idempotente?",
                "PUT substitui o recurso inteiro.",
                options,
                Set.of("http"),
                true);
    }

    @Test
    void exposesTheSingleCorrectOption() {
        Question q =
                question(
                        List.of(
                                option("POST", false, 0),
                                option("PUT", true, 1),
                                option("PATCH", false, 2)));

        assertThat(q.correctOption().text()).isEqualTo("PUT");
        assertThat(q.options()).hasSize(3);
    }

    @Test
    void rejectsZeroOrTwoCorrectOptions() {
        assertThatThrownBy(() -> question(List.of(option("A", false, 0), option("B", false, 1))))
                .isInstanceOf(InvalidQuestionException.class);
        assertThatThrownBy(() -> question(List.of(option("A", true, 0), option("B", true, 1))))
                .isInstanceOf(InvalidQuestionException.class);
    }

    @Test
    void rejectsTooFewOptionsDuplicatePositionsBlankTextAndBadSlug() {
        assertThatThrownBy(() -> question(List.of(option("A", true, 0))))
                .isInstanceOf(InvalidQuestionException.class);
        assertThatThrownBy(() -> question(List.of(option("A", true, 0), option("B", false, 0))))
                .isInstanceOf(InvalidQuestionException.class);
        assertThatThrownBy(() -> question(List.of(option(" ", true, 0), option("B", false, 1))))
                .isInstanceOf(InvalidQuestionException.class);
        assertThatThrownBy(
                        () ->
                                new Question(
                                        UUID.randomUUID(),
                                        "Bad Slug",
                                        "Título",
                                        UUID.randomUUID(),
                                        Level.JUNIOR,
                                        Difficulty.EASY,
                                        "x",
                                        "y",
                                        List.of(option("A", true, 0), option("B", false, 1)),
                                        Set.of(),
                                        true))
                .isInstanceOf(CatalogException.class);
    }

    @Test
    void requiresTitleAndDifficulty() {
        List<QuestionOption> options = List.of(option("A", true, 0), option("B", false, 1));
        assertThatThrownBy(
                        () ->
                                new Question(
                                        UUID.randomUUID(),
                                        "q-sem-titulo",
                                        " ",
                                        UUID.randomUUID(),
                                        Level.JUNIOR,
                                        Difficulty.EASY,
                                        "s",
                                        "e",
                                        options,
                                        Set.of(),
                                        true))
                .isInstanceOf(CatalogException.class)
                .hasMessageContaining("título");
        assertThatThrownBy(
                        () ->
                                new Question(
                                        UUID.randomUUID(),
                                        "q-sem-dificuldade",
                                        "Título",
                                        UUID.randomUUID(),
                                        Level.JUNIOR,
                                        null,
                                        "s",
                                        "e",
                                        options,
                                        Set.of(),
                                        true))
                .isInstanceOf(InvalidQuestionException.class)
                .hasMessageContaining("dificuldade");
    }
}
