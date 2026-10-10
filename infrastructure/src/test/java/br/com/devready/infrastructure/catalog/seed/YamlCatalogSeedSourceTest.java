package br.com.devready.infrastructure.catalog.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.catalog.dto.CatalogSeed;
import br.com.devready.application.catalog.exception.InvalidSeedException;
import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.core.JacksonException;

class YamlCatalogSeedSourceTest {

    @Test
    void readsAreasTagsQuestionsAndChallengesFromYaml() {
        CatalogSeed seed = new YamlCatalogSeedSource("classpath:seed-fixtures").load();

        assertThat(seed.areas()).hasSize(1);
        assertThat(seed.areas().get(0).objectiveWeight()).isEqualTo(0.5);
        assertThat(seed.tags()).extracting(t -> t.slug()).containsExactly("http", "arrays");

        var question = seed.questions().get(0);
        assertThat(question.slug()).isEqualTo("backend-junior-http-idempotency");
        assertThat(question.areaSlug()).isEqualTo("backend");
        assertThat(question.level()).isEqualTo(Level.JUNIOR);
        assertThat(question.title()).isEqualTo("Idempotência dos métodos HTTP");
        assertThat(question.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(question.statement()).startsWith("Qual método HTTP");
        assertThat(question.options()).hasSize(3);
        assertThat(question.options().get(1).correct()).isTrue();
        assertThat(question.options().get(0).correct()).isFalse();
        assertThat(question.tags()).containsExactly("http");

        var challenge = seed.challenges().get(0);
        assertThat(challenge.slug()).isEqualTo("two-sum");
        assertThat(challenge.level()).isEqualTo(Level.JUNIOR);
        assertThat(challenge.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(challenge.comparison()).isEqualTo(ComparisonMode.UNORDERED);
        assertThat(challenge.maxSubmissions()).isEqualTo(5);
        assertThat(challenge.languages()).containsKeys(Language.JAVA, Language.PYTHON);
        assertThat(challenge.languages().get(Language.PYTHON).functionName()).isEqualTo("two_sum");
        assertThat(challenge.languages().get(Language.JAVA).referenceSolution())
                .contains("HashMap");
        assertThat(challenge.testCases()).hasSize(4);
        assertThat(challenge.testCases().get(0).argsJson()).isEqualTo("[[2,7,11,15],9]");
        assertThat(challenge.testCases().get(0).expectedJson()).isEqualTo("[0,1]");
        assertThat(challenge.testCases().get(2).visible()).isFalse();
    }

    @Test
    void malformedFileNamesTheFileAndKeepsTheCause(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("areas.yml"), "- { slug: backend, name: \"Backend }\n");
        Files.writeString(dir.resolve("tags.yml"), "[]\n");

        assertThatThrownBy(() -> new YamlCatalogSeedSource("file:" + dir.toAbsolutePath()).load())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("areas.yml")
                .hasCauseInstanceOf(JacksonException.class);
    }

    @Test
    void malformedQuestionFileNamesTheFileAndKeepsTheCause(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("areas.yml"), "[]\n");
        Files.writeString(dir.resolve("tags.yml"), "[]\n");
        Files.createDirectories(dir.resolve("questions"));
        Files.writeString(
                dir.resolve("questions/backend-junior.yml"), "area: backend\nlevel: \"junior\n");

        assertThatThrownBy(() -> new YamlCatalogSeedSource("file:" + dir.toAbsolutePath()).load())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("backend-junior.yml")
                .hasCauseInstanceOf(JacksonException.class);
    }

    @Test
    void questionWithoutDifficultyNamesTheFileAndTheSlug(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("areas.yml"), "[]\n");
        Files.writeString(dir.resolve("tags.yml"), "[]\n");
        Files.createDirectories(dir.resolve("questions"));
        Files.writeString(
                dir.resolve("questions/backend-junior.yml"),
                """
                area: backend
                level: junior
                questions:
                  - slug: sem-dificuldade
                    title: Sem dificuldade
                    statement: s
                    options: [{ text: a, correct: true }, { text: b }]
                    explanation: e
                """);

        assertThatThrownBy(() -> new YamlCatalogSeedSource("file:" + dir.toAbsolutePath()).load())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("backend-junior.yml")
                .hasMessageContaining("sem-dificuldade");
    }

    private static final String CHALLENGE =
            """
            slug: soma
            title: Soma
            area: backend
            level: %s
            difficulty: easy
            comparison: %s
            statement: s
            approachSummary: a
            languages:
              %s:
                functionName: soma
                starterCode: x
            testCases:
              - { args: [1], expected: 1, visible: true }
            """;

    private static String seedDir(Path dir, String questionLevel, String challenge)
            throws IOException {
        Files.writeString(dir.resolve("areas.yml"), "[]");
        Files.writeString(dir.resolve("tags.yml"), "[]");
        Files.createDirectories(dir.resolve("questions"));
        Files.createDirectories(dir.resolve("challenges"));
        Files.writeString(
                dir.resolve("questions/backend-x.yml"),
                "area: backend\nlevel: " + questionLevel + "\nquestions: []\n");
        Files.writeString(dir.resolve("challenges/soma.yml"), challenge);
        return "file:" + dir.toAbsolutePath();
    }

    @Test
    void unknownQuestionLevelNamesTheFileAndTheValue(@TempDir Path dir) throws IOException {
        String location = seedDir(dir, "mestre", CHALLENGE.formatted("junior", "exact", "java"));

        assertThatThrownBy(() -> new YamlCatalogSeedSource(location).load())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("backend-x.yml")
                .hasMessageContaining("mestre");
    }

    @Test
    void unknownComparisonModeNamesTheFileAndTheValue(@TempDir Path dir) throws IOException {
        String location = seedDir(dir, "junior", CHALLENGE.formatted("junior", "fuzzy", "java"));

        assertThatThrownBy(() -> new YamlCatalogSeedSource(location).load())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("soma.yml")
                .hasMessageContaining("fuzzy");
    }

    @Test
    void unknownLanguageNamesTheFileAndTheValue(@TempDir Path dir) throws IOException {
        String location = seedDir(dir, "junior", CHALLENGE.formatted("junior", "exact", "cobol"));

        assertThatThrownBy(() -> new YamlCatalogSeedSource(location).load())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("soma.yml")
                .hasMessageContaining("cobol");
    }

    @Test
    void missingComparisonModeNamesTheFile(@TempDir Path dir) throws IOException {
        String location =
                seedDir(
                        dir,
                        "junior",
                        CHALLENGE
                                .formatted("junior", "exact", "java")
                                .replace("comparison: exact\n", ""));

        assertThatThrownBy(() -> new YamlCatalogSeedSource(location).load())
                .isInstanceOf(InvalidSeedException.class)
                .hasMessageContaining("soma.yml");
    }
}
