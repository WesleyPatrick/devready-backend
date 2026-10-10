package br.com.devready.domain.practice;

import static br.com.devready.domain.practice.AnswerFixtures.DAY;
import static br.com.devready.domain.practice.AnswerFixtures.at;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PracticeCalendarTest {

    @Test
    void theZoneIsBrasilia() {
        assertThat(PracticeCalendar.ZONE.getId()).isEqualTo("America/Sao_Paulo");
    }

    @Test
    void theDayTurnsAtMidnightInBrasilia() {
        assertThat(PracticeCalendar.dayOf(at(0, 23, 59))).isEqualTo(DAY);
        assertThat(PracticeCalendar.dayOf(at(1, 0, 0))).isEqualTo(DAY.plusDays(1));
        assertThat(PracticeCalendar.dayOf(DAY.plusDays(1).atTime(2, 59).toInstant(ZoneOffset.UTC)))
                .isEqualTo(DAY);
    }

    @Test
    void theDayStartsAtMidnightInBrasilia() {
        assertThat(PracticeCalendar.startOf(DAY))
                .isEqualTo(DAY.atStartOfDay().toInstant(ZoneOffset.ofHours(-3)));
    }
}
