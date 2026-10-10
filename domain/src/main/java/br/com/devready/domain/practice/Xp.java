package br.com.devready.domain.practice;

import br.com.devready.domain.catalog.Difficulty;

public final class Xp {

    private Xp() {}

    public static int firstSolve(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 20;
            case MEDIUM -> 30;
            case HARD -> 40;
        };
    }

    public static int gained(Difficulty difficulty, boolean correct, boolean solvedBefore) {
        return correct && !solvedBefore ? firstSolve(difficulty) : 0;
    }

    public static int challengeFirstSolve(Difficulty difficulty) {
        return 2 * firstSolve(difficulty);
    }
}
