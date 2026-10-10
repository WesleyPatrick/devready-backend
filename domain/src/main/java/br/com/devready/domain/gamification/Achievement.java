package br.com.devready.domain.gamification;

import java.util.Arrays;
import java.util.Optional;

public enum Achievement {
    FIRST_QUESTION(
            "primeira-questao", "Primeira questão", "Respondeu a primeira questão.", "check"),
    TEN_QUESTIONS("dez-questoes", "Dez questões", "Resolveu dez questões.", "check-check"),
    STREAK_3("sequencia-3", "Três dias seguidos", "Praticou três dias seguidos.", "flame"),
    STREAK_7("sequencia-7", "Uma semana seguida", "Praticou sete dias seguidos.", "flame"),
    STREAK_30("sequencia-30", "Um mês seguido", "Praticou trinta dias seguidos.", "calendar"),
    XP_1000("mil-xp", "Mil XP", "Chegou a 1.000 pontos de experiência.", "zap"),
    XP_5000("cinco-mil-xp", "Cinco mil XP", "Chegou a 5.000 pontos de experiência.", "trophy"),
    GOAL_FIVE_TIMES(
            "meta-cinco-vezes", "Meta em dia", "Cumpriu a meta do dia cinco vezes.", "target"),
    FIRST_COMPETENCY(
            "primeira-competencia",
            "Primeira competência",
            "Concluiu uma competência da trilha.",
            "book"),
    FIRST_SIMULATION(
            "primeira-simulacao",
            "Primeira simulação",
            "Terminou uma simulação cronometrada.",
            "timer");

    private final String slug;
    private final String displayName;
    private final String description;
    private final String icon;

    Achievement(String slug, String displayName, String description, String icon) {
        this.slug = slug;
        this.displayName = displayName;
        this.description = description;
        this.icon = icon;
    }

    public String slug() {
        return slug;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public String icon() {
        return icon;
    }

    public static Optional<Achievement> fromSlug(String slug) {
        return Arrays.stream(values()).filter(a -> a.slug.equals(slug)).findFirst();
    }
}
