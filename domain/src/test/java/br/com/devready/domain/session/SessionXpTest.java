package br.com.devready.domain.session;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.catalog.Level;
import org.junit.jupiter.api.Test;

class SessionXpTest {

    @Test
    void isTheScoreTimesOneHundredTimesTheMultiplierOfTheLevel() {
        assertThat(SessionXp.of(0.5, Level.JUNIOR)).isEqualTo(50);
        assertThat(SessionXp.of(0.5, Level.PLENO)).isEqualTo(75);
        assertThat(SessionXp.of(1.0, Level.SENIOR)).isEqualTo(200);
    }

    @Test
    void roundsTheExactHalfUp() {
        assertThat(SessionXp.of(0.725, Level.JUNIOR)).isEqualTo(73);
        assertThat(SessionXp.of(0.6667, Level.PLENO)).isEqualTo(100);
    }

    @Test
    void neverGivesLessThanTen() {
        assertThat(SessionXp.of(0.0, Level.JUNIOR)).isEqualTo(10);
        assertThat(SessionXp.of(0.05, Level.SENIOR)).isEqualTo(10);
        assertThat(SessionXp.of(0.11, Level.JUNIOR)).isEqualTo(11);
    }
}
