package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;
import br.com.devready.domain.catalog.Level;

public class InsufficientPoolException extends ApplicationException {
    public InsufficientPoolException(String areaName, Level level) {
        super(
                "Não há questões e desafios suficientes para uma simulação de "
                        + areaName
                        + " "
                        + label(level)
                        + ".");
    }

    private static String label(Level level) {
        return switch (level) {
            case JUNIOR -> "júnior";
            case PLENO -> "pleno";
            case SENIOR -> "sênior";
        };
    }
}
