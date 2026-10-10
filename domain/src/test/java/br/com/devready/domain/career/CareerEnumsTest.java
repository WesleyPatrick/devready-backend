package br.com.devready.domain.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CareerEnumsTest {

    @Test
    void categoriesFollowTheStudyOrderAndReadTheResearchSlugs() {
        assertThat(CompetencyCategory.values())
                .containsExactly(
                        CompetencyCategory.FUNDAMENTO,
                        CompetencyCategory.LINGUAGEM,
                        CompetencyCategory.FRAMEWORK,
                        CompetencyCategory.PRATICA,
                        CompetencyCategory.COMPORTAMENTAL);
        assertThat(CompetencyCategory.fromSlug("pratica")).isEqualTo(CompetencyCategory.PRATICA);
        assertThat(CompetencyCategory.COMPORTAMENTAL.slug()).isEqualTo("comportamental");
        assertThatThrownBy(() -> CompetencyCategory.fromSlug("dados"))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("dados");
    }

    @Test
    void requirementReadsTheResearchSlugs() {
        assertThat(Requirement.fromSlug("obrigatoria")).isEqualTo(Requirement.OBRIGATORIA);
        assertThat(Requirement.fromSlug("desejavel")).isEqualTo(Requirement.DESEJAVEL);
        assertThat(Requirement.DESEJAVEL.slug()).isEqualTo("desejavel");
        assertThatThrownBy(() -> Requirement.fromSlug("opcional"))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("opcional");
    }

    @Test
    void materialTypeReadsTheResearchSlugs() {
        assertThat(MaterialType.fromSlug("documentacao")).isEqualTo(MaterialType.DOCUMENTACAO);
        assertThat(MaterialType.fromSlug("video")).isEqualTo(MaterialType.VIDEO);
        assertThat(MaterialType.LIVRO.slug()).isEqualTo("livro");
        assertThatThrownBy(() -> MaterialType.fromSlug("podcast"))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("podcast");
    }
}
