package br.com.devready.domain.practice;

import br.com.devready.domain.catalog.Difficulty;

public final class EstimatedMinutes {

    private EstimatedMinutes() {}

    public static int of(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 2;
            case MEDIUM -> 4;
            case HARD -> 6;
        };
    }
}
