package br.com.devready.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class LevelTest {

    @Test
    void juniorMultipliesXpByOne() {
        assertThat(Level.JUNIOR.xpMultiplier()).isEqualTo(1.0);
    }

    @Test
    void plenoMultipliesXpByOneAndAHalf() {
        assertThat(Level.PLENO.xpMultiplier()).isEqualTo(1.5);
    }

    @Test
    void seniorMultipliesXpByTwo() {
        assertThat(Level.SENIOR.xpMultiplier()).isEqualTo(2.0);
    }

    @Test
    void levelsAreOrderedFromJuniorToSenior() {
        assertThat(Level.values()).containsExactly(Level.JUNIOR, Level.PLENO, Level.SENIOR);
    }

    @Test
    void upToIsTheLevelAndTheOnesBelowIt() {
        assertThat(Level.upTo(Level.JUNIOR)).containsExactly(Level.JUNIOR);
        assertThat(Level.upTo(Level.PLENO)).containsExactly(Level.JUNIOR, Level.PLENO);
        assertThat(Level.upTo(Level.SENIOR))
                .containsExactly(Level.JUNIOR, Level.PLENO, Level.SENIOR);
    }

    @Test
    void upToCannotBeChangedNorAskedWithoutASeniority() {
        assertThatThrownBy(() -> Level.upTo(Level.PLENO).add(Level.SENIOR))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> Level.upTo(null))
                .isInstanceOf(CatalogException.class)
                .hasMessage("Informe a senioridade para saber os níveis visíveis.");
    }
}
