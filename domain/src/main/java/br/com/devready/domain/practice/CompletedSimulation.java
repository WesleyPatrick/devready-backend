package br.com.devready.domain.practice;

import java.time.Instant;

public record CompletedSimulation(Instant finishedAt, int xpAwarded) {
    public CompletedSimulation {
        if (finishedAt == null || xpAwarded < 0) {
            throw new PracticeException("Simulação corrigida sem hora do fim ou com XP negativo.");
        }
    }

    public boolean counts() {
        return xpAwarded > 0;
    }
}
