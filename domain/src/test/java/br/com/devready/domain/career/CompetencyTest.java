package br.com.devready.domain.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.domain.catalog.Difficulty;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CompetencyTest {

    private static Material material(String title) {
        return new Material(title, "https://exemplo.com", MaterialType.LIVRO, "pt", true);
    }

    @Test
    void normalisesOptionalFieldsAndCopiesCollections() {
        Set<String> tags = new HashSet<>(Set.of("sql"));
        List<Material> materials = new ArrayList<>(List.of(material("Um")));
        Competency competency =
                new Competency(
                        UUID.randomUUID(),
                        "sql-e-modelagem-de-dados",
                        "SQL e modelagem de dados",
                        CompetencyCategory.FUNDAMENTO,
                        "  Consultas e índices.\n",
                        Difficulty.MEDIUM,
                        tags,
                        materials);

        tags.add("java");
        materials.add(material("Dois"));

        assertThat(competency.summary()).isEqualTo("Consultas e índices.");
        assertThat(competency.tagSlugs()).containsExactly("sql");
        assertThat(competency.materials()).extracting(Material::title).containsExactly("Um");

        Competency bare =
                new Competency(
                        UUID.randomUUID(),
                        "ingles",
                        "Inglês",
                        CompetencyCategory.COMPORTAMENTAL,
                        null,
                        Difficulty.MEDIUM,
                        null,
                        null);
        assertThat(bare.summary()).isEmpty();
        assertThat(bare.tagSlugs()).isEmpty();
        assertThat(bare.materials()).isEmpty();
    }

    @Test
    void requiresIdSlugNameCategoryDifficultyAndValidTags() {
        UUID id = UUID.randomUUID();
        CompetencyCategory category = CompetencyCategory.FUNDAMENTO;
        assertThatThrownBy(
                        () ->
                                new Competency(
                                        null,
                                        "sql",
                                        "SQL",
                                        category,
                                        "",
                                        Difficulty.MEDIUM,
                                        null,
                                        null))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(
                        () ->
                                new Competency(
                                        id,
                                        "SQL e Dados",
                                        "SQL",
                                        category,
                                        "",
                                        Difficulty.MEDIUM,
                                        null,
                                        null))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(
                        () ->
                                new Competency(
                                        id,
                                        "sql",
                                        " ",
                                        category,
                                        "",
                                        Difficulty.MEDIUM,
                                        null,
                                        null))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(
                        () ->
                                new Competency(
                                        id, "sql", "SQL", null, "", Difficulty.MEDIUM, null, null))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("sem categoria");
        assertThatThrownBy(() -> new Competency(id, "sql", "SQL", category, "", null, null, null))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("sem dificuldade");
        assertThatThrownBy(
                        () ->
                                new Competency(
                                        id,
                                        "sql",
                                        "SQL",
                                        category,
                                        "",
                                        Difficulty.MEDIUM,
                                        Set.of("Banco de Dados"),
                                        null))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("Banco de Dados");
    }

    @Test
    void materialNeedsTitleHttpUrlTypeAndPortugueseOrEnglish() {
        assertThatThrownBy(() -> new Material(" ", "https://x", MaterialType.LIVRO, "pt", true))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new Material("Livro", "ftp://x", MaterialType.LIVRO, "pt", true))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("URL");
        assertThatThrownBy(() -> new Material("Livro", "https://x", null, "pt", true))
                .isInstanceOf(CareerException.class);
        assertThatThrownBy(() -> new Material("Livro", "https://x", MaterialType.LIVRO, "es", true))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("es");
        assertThatThrownBy(() -> new Material("Livro", "https://x", MaterialType.LIVRO, null, true))
                .isInstanceOf(CareerException.class);
        assertThat(new Material("  Livro ", "http://x", MaterialType.VIDEO, "en", false).title())
                .isEqualTo("Livro");
    }
}
