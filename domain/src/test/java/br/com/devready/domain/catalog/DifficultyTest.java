package br.com.devready.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DifficultyTest {

    @Test
    void readsTheSlugsUsedInTheSeedFiles() {
        assertThat(Difficulty.fromSlug("easy")).isEqualTo(Difficulty.EASY);
        assertThat(Difficulty.fromSlug("medium")).isEqualTo(Difficulty.MEDIUM);
        assertThat(Difficulty.fromSlug("hard")).isEqualTo(Difficulty.HARD);
        assertThat(Difficulty.HARD.slug()).isEqualTo("hard");
    }

    @Test
    void isOrderedFromEasyToHard() {
        assertThat(Difficulty.values())
                .containsExactly(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD);
    }

    @Test
    void rejectsUnknownDifficulty() {
        assertThatThrownBy(() -> Difficulty.fromSlug("dificil"))
                .isInstanceOf(CatalogException.class)
                .hasMessageContaining("dificil");
        assertThatThrownBy(() -> Difficulty.fromSlug(null)).isInstanceOf(CatalogException.class);
    }
}
