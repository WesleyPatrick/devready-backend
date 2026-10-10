package br.com.devready.infrastructure.gamification.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.QuestionOption;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.UnlockedAchievement;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.session.Session;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.catalog.persistence.JpaAreaRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaQuestionRepository;
import br.com.devready.infrastructure.identity.persistence.JpaUserRepository;
import br.com.devready.infrastructure.session.persistence.JpaSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
    PostgresTestcontainersConfiguration.class,
    JpaUserRepository.class,
    JpaAreaRepository.class,
    JpaQuestionRepository.class,
    JpaSessionRepository.class,
    JpaAchievementRepository.class
})
class JpaAchievementRepositoryTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    @Autowired private JpaUserRepository users;
    @Autowired private JpaAreaRepository areas;
    @Autowired private JpaQuestionRepository questions;
    @Autowired private JpaSessionRepository sessions;
    @Autowired private JpaAchievementRepository achievements;
    @Autowired private TestEntityManager entityManager;

    private UUID user() {
        UUID id = UUID.randomUUID();
        users.save(User.register(id, "conq-" + id + "@example.com", "Conquista", "hash", T0));
        return id;
    }

    private Session lesson(UUID user) {
        String s = UUID.randomUUID().toString().substring(0, 8);
        Area area =
                areas.save(
                        new Area(
                                UUID.randomUUID(),
                                "zz-conq-" + s,
                                "Conquistas",
                                new SessionConfig(1, 0, 10, 1.0),
                                true));
        List<UUID> ids =
                List.of(1, 2, 3).stream()
                        .map(
                                i ->
                                        questions
                                                .save(
                                                        new Question(
                                                                UUID.randomUUID(),
                                                                "zz-conq-" + s + "-" + i,
                                                                "Título",
                                                                area.id(),
                                                                Level.JUNIOR,
                                                                Difficulty.EASY,
                                                                "Enunciado",
                                                                "Explicação",
                                                                List.of(
                                                                        new QuestionOption(
                                                                                UUID.randomUUID(),
                                                                                "A",
                                                                                false,
                                                                                0),
                                                                        new QuestionOption(
                                                                                UUID.randomUUID(),
                                                                                "B",
                                                                                true,
                                                                                1)),
                                                                Set.of(),
                                                                true))
                                                .id())
                        .toList();
        return sessions.create(
                Session.lesson(UUID.randomUUID(), user, area.id(), Level.JUNIOR, "sql", ids, T0));
    }

    @Test
    void anAchievementIsUnlockedOnceAndTheFirstTimeStays() {
        UUID ana = user();

        assertThat(
                        achievements.unlock(
                                new UnlockedAchievement(ana, Achievement.STREAK_3, T0, null)))
                .isTrue();
        assertThat(
                        achievements.unlock(
                                new UnlockedAchievement(
                                        ana, Achievement.STREAK_3, T0.plusSeconds(60), null)))
                .isFalse();
        entityManager.clear();

        assertThat(achievements.findByUser(ana))
                .containsExactly(new UnlockedAchievement(ana, Achievement.STREAK_3, T0, null));
        assertThat(achievements.findByUser(UUID.randomUUID())).isEmpty();
    }

    @Test
    void whatACorrectionUnlockedKeepsTheSession() {
        UUID bia = user();
        Session session = lesson(bia);
        achievements.unlock(
                new UnlockedAchievement(bia, Achievement.FIRST_SIMULATION, T0, session.id()));
        achievements.unlock(
                new UnlockedAchievement(bia, Achievement.FIRST_QUESTION, T0, session.id()));
        achievements.unlock(new UnlockedAchievement(bia, Achievement.XP_1000, T0, null));
        entityManager.clear();

        assertThat(achievements.unlockedBySession(bia, session.id()))
                .containsExactly(Achievement.FIRST_QUESTION, Achievement.FIRST_SIMULATION);
        assertThat(achievements.findByUser(bia)).hasSize(3);
    }
}
