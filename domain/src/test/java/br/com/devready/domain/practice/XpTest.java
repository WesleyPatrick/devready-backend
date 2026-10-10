package br.com.devready.domain.practice;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.catalog.Difficulty;
import org.junit.jupiter.api.Test;

class XpTest {

    @Test
    void theFirstSolveIsWorthTwentyThirtyOrFortyByDifficulty() {
        assertThat(Xp.firstSolve(Difficulty.EASY)).isEqualTo(20);
        assertThat(Xp.firstSolve(Difficulty.MEDIUM)).isEqualTo(30);
        assertThat(Xp.firstSolve(Difficulty.HARD)).isEqualTo(40);
    }

    @Test
    void onlyTheFirstRightAnswerEarnsXp() {
        assertThat(Xp.gained(Difficulty.MEDIUM, true, false)).isEqualTo(30);
        assertThat(Xp.gained(Difficulty.MEDIUM, true, true)).isZero();
        assertThat(Xp.gained(Difficulty.HARD, false, false)).isZero();
        assertThat(Xp.gained(Difficulty.HARD, false, true)).isZero();
    }
}
