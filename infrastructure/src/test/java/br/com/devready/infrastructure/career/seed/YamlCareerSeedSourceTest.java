package br.com.devready.infrastructure.career.seed;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.career.dto.CareerSeed;
import br.com.devready.application.career.dto.CompanySeed;
import br.com.devready.application.career.dto.CompetencySeed;
import br.com.devready.application.career.dto.MaterialSeed;
import br.com.devready.application.career.dto.StackSeed;
import br.com.devready.application.career.dto.TrackSeed;
import br.com.devready.application.career.dto.TrackUnitSeed;
import br.com.devready.application.career.exception.InvalidCareerSeedException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.core.JacksonException;

class YamlCareerSeedSourceTest {

    @Test
    void readsCompaniesAndStacksFromYaml() {
        CareerSeed seed = new YamlCareerSeedSource("classpath:career-fixtures").load();

        assertThat(seed.companies())
                .containsExactly(
                        new CompanySeed(
                                "nubank",
                                "Nubank",
                                "tech-brasil",
                                "Processo com triagem, entrevista técnica e conversa com o time.\n",
                                List.of(
                                        "https://jobs.ashbyhq.com/nubank",
                                        "https://nubank.com.br/carreiras")),
                        new CompanySeed("google", "Google", "big-tech", "", List.of()));
        assertThat(seed.stacks())
                .containsExactly(
                        new StackSeed("java-spring", "Java e Spring", "backend"),
                        new StackSeed("react", "React", "frontend"));
    }

    @Test
    void missingFilesMeanNothingToSeed() {
        CareerSeed seed = new YamlCareerSeedSource("classpath:nao-existe").load();

        assertThat(seed.companies()).isEmpty();
        assertThat(seed.stacks()).isEmpty();
        assertThat(seed.competencies()).isEmpty();
        assertThat(seed.materials()).isEmpty();
        assertThat(seed.tracks()).isEmpty();
    }

    @Test
    void readsCompetenciesMaterialsAndTracksFromYaml() {
        CareerSeed seed = new YamlCareerSeedSource("classpath:career-fixtures").load();

        assertThat(seed.competencies())
                .containsExactly(
                        new CompetencySeed(
                                "sql-e-modelagem-de-dados",
                                "SQL e modelagem de dados",
                                "fundamento",
                                "Consultas, índices e modelagem.\n",
                                "medium",
                                List.of("sql")),
                        new CompetencySeed(
                                "ingles", "Inglês", "comportamental", "", "medium", List.of()));
        assertThat(seed.materials())
                .containsExactly(
                        new MaterialSeed(
                                "sql-e-modelagem-de-dados",
                                "Use The Index, Luke",
                                "https://use-the-index-luke.com/sql/table-of-contents",
                                "livro",
                                "en",
                                true));
        assertThat(seed.tracks())
                .containsExactly(
                        new TrackSeed(
                                "google",
                                "backend",
                                "java-spring",
                                "junior",
                                5,
                                List.of(
                                        new TrackUnitSeed(
                                                "sql-e-modelagem-de-dados", 0.4, "desejavel", 2))),
                        new TrackSeed(
                                "nubank",
                                "backend",
                                "todas",
                                "todas",
                                17,
                                List.of(
                                        new TrackUnitSeed(
                                                "sql-e-modelagem-de-dados", 0.41, "obrigatoria", 7),
                                        new TrackUnitSeed("ingles", 0.59, "obrigatoria", 10))));
    }

    @Test
    void trackFileWithoutTracksOrOnlyNullAddsNothing(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("tracks"));
        Files.writeString(dir.resolve("tracks/meta.yml"), "company: meta\ntracks: []\n");
        Files.writeString(dir.resolve("tracks/vazio.yml"), "---\n");

        CareerSeed seed = new YamlCareerSeedSource("file:" + dir.toAbsolutePath()).load();

        assertThat(seed.tracks()).isEmpty();
    }

    @Test
    void malformedTrackFileNamesTheFile(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("tracks"));
        Files.writeString(dir.resolve("tracks/nubank.yml"), "company: \"nubank\ntracks: []\n");

        assertThatThrownBy(() -> new YamlCareerSeedSource("file:" + dir.toAbsolutePath()).load())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("nubank.yml")
                .hasCauseInstanceOf(JacksonException.class);
    }

    @Test
    void unknownFieldInATrackFileIsAnErrorThatNamesTheFile(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("tracks"));
        Files.writeString(
                dir.resolve("tracks/nubank.yml"), "company: nubank\nsmaple: true\ntracks: []\n");

        assertThatThrownBy(() -> new YamlCareerSeedSource("file:" + dir.toAbsolutePath()).load())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("nubank.yml")
                .hasMessageContaining("smaple");
    }

    @Test
    void unknownFieldInAListFileIsAnErrorThatNamesTheFile(@TempDir Path dir) throws IOException {
        Files.writeString(
                dir.resolve("competencies.yml"),
                "- { slug: java, name: Java, category: linguagem, summary: s, difficulty: easy,"
                        + " dificuldade: hard, tags: [java] }\n");

        assertThatThrownBy(() -> new YamlCareerSeedSource("file:" + dir.toAbsolutePath()).load())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("competencies.yml")
                .hasMessageContaining("dificuldade");
    }

    @Test
    void documentThatIsOnlyNullMeansNothingToSeed(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("companies.yml"), "---\n");
        Files.writeString(dir.resolve("stacks.yml"), "null\n");

        CareerSeed seed = new YamlCareerSeedSource("file:" + dir.toAbsolutePath()).load();

        assertThat(seed.companies()).isEmpty();
        assertThat(seed.stacks()).isEmpty();
    }

    @Test
    void emptyOrCommentOnlyFileNamesTheFile(@TempDir Path dir) throws IOException {
        Path companies = dir.resolve("companies.yml");
        String location = "file:" + dir.toAbsolutePath();

        Files.writeString(companies, "# só um comentário, nenhuma empresa\n");
        assertThatThrownBy(() -> new YamlCareerSeedSource(location).load())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("companies.yml");

        Files.writeString(companies, "");
        assertThatThrownBy(() -> new YamlCareerSeedSource(location).load())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("companies.yml");
    }

    @Test
    void malformedFileNamesTheFile(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("stacks.yml"), "- { slug: go, name: \"Go }\n");

        assertThatThrownBy(() -> new YamlCareerSeedSource("file:" + dir.toAbsolutePath()).load())
                .isInstanceOf(InvalidCareerSeedException.class)
                .hasMessageContaining("stacks.yml")
                .hasCauseInstanceOf(JacksonException.class);
    }
}
