package br.com.devready.domain.practice;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.catalog.Difficulty;
import org.junit.jupiter.api.Test;

class EstimatedMinutesTest {

    @Test
    void twoFourOrSixMinutesByDifficulty() {
        assertThat(EstimatedMinutes.of(Difficulty.EASY)).isEqualTo(2);
        assertThat(EstimatedMinutes.of(Difficulty.MEDIUM)).isEqualTo(4);
        assertThat(EstimatedMinutes.of(Difficulty.HARD)).isEqualTo(6);
    }
}
