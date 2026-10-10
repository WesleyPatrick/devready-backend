package br.com.devready.domain.practice;

import java.time.LocalDate;

public record DayActivity(LocalDate date, int count) {
    public DayActivity {
        if (date == null || count < 0) {
            throw new PracticeException("Dia de atividade inválido.");
        }
    }
}
