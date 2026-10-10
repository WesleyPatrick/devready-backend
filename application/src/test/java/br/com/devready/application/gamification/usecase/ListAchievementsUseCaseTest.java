package br.com.devready.application.gamification.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.gamification.dto.AchievementView;
import br.com.devready.application.gamification.fake.InMemoryAchievementRepository;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.UnlockedAchievement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListAchievementsUseCaseTest {

    @Test
    void theTenInTheOrderOfTheCatalogWithTheTimeOfTheUnlocked() {
        InMemoryAchievementRepository achievements = new InMemoryAchievementRepository();
        UUID user = UUID.randomUUID();
        Instant when = Instant.ofEpochSecond(1_700_000_000L);
        achievements.unlock(new UnlockedAchievement(user, Achievement.XP_1000, when, null));
        achievements.unlock(
                new UnlockedAchievement(UUID.randomUUID(), Achievement.STREAK_3, when, null));

        List<AchievementView> views = new ListAchievementsUseCase(achievements).execute(user);

        assertThat(views)
                .extracting(AchievementView::achievement)
                .containsExactly(Achievement.values());
        assertThat(views)
                .filteredOn(view -> view.unlockedAt() != null)
                .extracting(AchievementView::achievement)
                .containsExactly(Achievement.XP_1000);
        assertThat(views.get(Achievement.XP_1000.ordinal()).unlockedAt()).isEqualTo(when);
    }
}
