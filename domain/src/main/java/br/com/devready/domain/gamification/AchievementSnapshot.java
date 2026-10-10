package br.com.devready.domain.gamification;

public record AchievementSnapshot(
        int answers,
        int solvedQuestions,
        int streakDays,
        int xp,
        int goalDays,
        boolean competencyDone,
        int completedSimulations) {
    public AchievementSnapshot {
        if (answers < 0
                || solvedQuestions < 0
                || streakDays < 0
                || xp < 0
                || goalDays < 0
                || completedSimulations < 0) {
            throw new GamificationException("Retrato das conquistas com contagem negativa.");
        }
    }
}
