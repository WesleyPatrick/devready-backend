package br.com.devready.domain.practice;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class PracticeCalendar {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    private PracticeCalendar() {}

    public static LocalDate dayOf(Instant instant) {
        return LocalDate.ofInstant(instant, ZONE);
    }

    public static Instant startOf(LocalDate day) {
        return day.atStartOfDay(ZONE).toInstant();
    }
}
