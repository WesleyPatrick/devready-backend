package br.com.devready.domain.gamification;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class AchievementTest {

    @Test
    void theCatalogHasTheTenSlugsInTheOrderOfTheScreen() {
        assertThat(Arrays.stream(Achievement.values()).map(Achievement::slug))
                .containsExactly(
                        "primeira-questao",
                        "dez-questoes",
                        "sequencia-3",
                        "sequencia-7",
                        "sequencia-30",
                        "mil-xp",
                        "cinco-mil-xp",
                        "meta-cinco-vezes",
                        "primeira-competencia",
                        "primeira-simulacao");
    }

    @Test
    void namesDescriptionsAndIconsAreTheOnesTheScreenAlreadyShows() {
        assertThat(Achievement.XP_1000.displayName()).isEqualTo("Mil XP");
        assertThat(Achievement.XP_1000.description())
                .isEqualTo("Chegou a 1.000 pontos de experiência.");
        assertThat(Achievement.XP_1000.icon()).isEqualTo("zap");
        assertThat(Achievement.GOAL_FIVE_TIMES.displayName()).isEqualTo("Meta em dia");
        assertThat(Achievement.FIRST_SIMULATION.description())
                .isEqualTo("Terminou uma simulação cronometrada.");
        assertThat(Achievement.FIRST_SIMULATION.icon()).isEqualTo("timer");
        assertThat(Achievement.STREAK_30.icon()).isEqualTo("calendar");
    }

    @Test
    void findsBySlug() {
        assertThat(Achievement.fromSlug("sequencia-7")).contains(Achievement.STREAK_7);
        assertThat(Achievement.fromSlug("sql-em-dia")).isEmpty();
        assertThat(Achievement.fromSlug("metade-da-trilha")).isEmpty();
    }
}
