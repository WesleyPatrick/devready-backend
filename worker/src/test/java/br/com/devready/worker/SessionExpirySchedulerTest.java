package br.com.devready.worker;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.gamification.port.AchievementRepository;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.session.port.SessionRepository;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.ChallengeLanguage;
import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.ExecutionLimits;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.UnlockedAchievement;
import br.com.devready.domain.identity.User;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionStatus;
import br.com.devready.worker.session.SessionExpiryScheduler;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "devready.sessions.expiry-delay-ms=3600000")
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class SessionExpirySchedulerTest {

    @Autowired private SessionExpiryScheduler scheduler;
    @Autowired private SessionRepository sessions;
    @Autowired private AchievementRepository achievements;
    @Autowired private AreaRepository areas;
    @Autowired private ChallengeRepository challenges;
    @Autowired private UserRepository users;

    private UUID user(String name) {
        UUID id = UUID.randomUUID();
        users.save(
                User.register(id, name + "-" + id + "@example.com", name, "hash", Instant.now()));
        return id;
    }

    private Challenge challenge() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Area area =
                areas.save(
                        new Area(
                                UUID.randomUUID(),
                                "zz-prazo-" + suffix,
                                "Prazo",
                                new SessionConfig(0, 1, 45, 0.5),
                                true));
        return challenges.save(
                new Challenge(
                        UUID.randomUUID(),
                        "zz-prazo-" + suffix,
                        "Prazo",
                        area.id(),
                        Level.JUNIOR,
                        Difficulty.EASY,
                        "Enunciado",
                        "Abordagem",
                        new ExecutionLimits(5000, 262144),
                        ComparisonMode.EXACT,
                        5,
                        Map.of(
                                Language.JAVA,
                                new ChallengeLanguage(
                                        Language.JAVA, "f", "class Solution {}", "ref")),
                        List.of(
                                new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                                new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                                new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                                new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                        Set.of(),
                        true));
    }

    private Session open(UUID user, Challenge challenge, Instant startedAt) {
        return sessions.create(
                Session.simulation(
                        UUID.randomUUID(),
                        user,
                        challenge.areaId(),
                        Level.PLENO,
                        new SessionConfig(0, 1, 45, 0.5),
                        List.of(),
                        List.of(challenge.id()),
                        startedAt));
    }

    @Test
    void aSimulationPastTheDeadlineIsCorrectedAndTheOneOnTimeStays(CapturedOutput output) {
        Challenge challenge = challenge();
        UUID late = user("atrasada");
        UUID onTime = user("no-prazo");
        Session past = open(late, challenge, Instant.now().minus(Duration.ofMinutes(46)));
        Session current = open(onTime, challenge, Instant.now());
        sessions.useSubmission(past.id(), 1, challenge.maxSubmissions());

        scheduler.expire();

        Session corrected = sessions.findById(past.id()).orElseThrow();
        assertThat(corrected.status()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(corrected.xpAwarded()).isEqualTo(10);
        assertThat(sessions.findById(current.id()).orElseThrow().status())
                .isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(achievements.findByUser(late))
                .extracting(UnlockedAchievement::achievement)
                .contains(Achievement.FIRST_SIMULATION);
        assertThat(achievements.findByUser(late))
                .filteredOn(u -> u.achievement() == Achievement.FIRST_SIMULATION)
                .extracting(UnlockedAchievement::sessionId)
                .containsExactly(past.id());
        assertThat(output.getOut())
                .contains("Sessões vencidas nesta rodada: ")
                .contains("Sessão " + past.id() + " fechada como COMPLETED")
                .doesNotContain(late.toString());
    }
}
