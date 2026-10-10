package br.com.devready.domain.career;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CompanyGroupTest {

    @Test
    void readsTheSlugsUsedInTheSeed() {
        assertThat(CompanyGroup.fromSlug("big-tech")).isEqualTo(CompanyGroup.BIG_TECH);
        assertThat(CompanyGroup.fromSlug("tech-brasil")).isEqualTo(CompanyGroup.TECH_BRASIL);
        assertThat(CompanyGroup.BIG_TECH.slug()).isEqualTo("big-tech");
    }

    @Test
    void rejectsUnknownGroup() {
        assertThatThrownBy(() -> CompanyGroup.fromSlug("startup"))
                .isInstanceOf(CareerException.class)
                .hasMessageContaining("startup");
    }
}
