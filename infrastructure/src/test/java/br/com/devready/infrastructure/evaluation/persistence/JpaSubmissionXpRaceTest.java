package br.com.devready.infrastructure.evaluation.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.devready.application.evaluation.exception.XpAlreadyGrantedException;
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
import br.com.devready.domain.evaluation.CodeSubmission;
import br.com.devready.domain.evaluation.SubmissionKind;
import br.com.devready.domain.evaluation.SubmissionStatus;
import br.com.devready.domain.identity.User;
import br.com.devready.infrastructure.PostgresTestcontainersConfiguration;
import br.com.devready.infrastructure.catalog.persistence.JpaAreaRepository;
import br.com.devready.infrastructure.catalog.persistence.JpaChallengeRepository;
import br.com.devready.infrastructure.identity.persistence.JpaUserRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Import({
    PostgresTestcontainersConfiguration.class,
    JpaAreaRepository.class,
    JpaChallengeRepository.class,
    JpaUserRepository.class,
    JpaSubmissionRepository.class
})
class JpaSubmissionXpRaceTest {

    private static final Instant T0 = Instant.ofEpochSecond(1_700_000_000L);

    @Autowired private JpaAreaRepository areas;
    @Autowired private JpaChallengeRepository challenges;
    @Autowired private JpaUserRepository users;
    @Autowired private JpaSubmissionRepository submissions;
    @Autowired private JdbcTemplate jdbc;

    private UUID userId;
    private UUID areaId;
    private UUID challengeId;

    @AfterEach
    void clean() {
        if (userId != null) {
            jdbc.update("delete from users where id = ?", userId);
        }
        if (challengeId != null) {
            jdbc.update("delete from challenges where id = ?", challengeId);
        }
        if (areaId != null) {
            jdbc.update("delete from areas where id = ?", areaId);
        }
    }

    @Test
    void theSecondAcceptedIsKeptWithZeroXpAfterTheViolation() {
        areaId = UUID.randomUUID();
        areas.save(new Area(areaId, "zz-corrida", "Área", new SessionConfig(8, 2, 45, 0.5), true));
        challengeId = UUID.randomUUID();
        challenges.save(challenge(challengeId, areaId));
        userId = UUID.randomUUID();
        users.save(User.register(userId, "corrida@example.com", "corrida", "hash", T0));
        submissions.save(accepted(UUID.randomUUID(), 40));
        CodeSubmission second = accepted(UUID.randomUUID(), 40);

        assertThatThrownBy(() -> submissions.save(second))
                .isInstanceOf(XpAlreadyGrantedException.class);
        submissions.save(accepted(second.id(), 0));

        assertThat(submissions.findById(second.id()).orElseThrow().xpGained()).isZero();
        assertThat(submissions.hasXpGained(userId, challengeId)).isTrue();
    }

    private CodeSubmission accepted(UUID id, int xpGained) {
        return new CodeSubmission(
                id,
                userId,
                challengeId,
                Language.JAVA,
                "class Solution {}",
                SubmissionKind.SUBMIT,
                SubmissionStatus.PASSED,
                1,
                1,
                null,
                null,
                40,
                xpGained,
                T0,
                T0,
                List.of(),
                null);
    }

    private static Challenge challenge(UUID id, UUID areaId) {
        return new Challenge(
                id,
                "zz-corrida-c",
                "Desafio",
                areaId,
                Level.JUNIOR,
                Difficulty.EASY,
                "Enunciado",
                "Abordagem",
                new ExecutionLimits(5000, 262144),
                ComparisonMode.EXACT,
                5,
                Map.of(
                        Language.JAVA,
                        new ChallengeLanguage(Language.JAVA, "f", "class Solution {}", "ref")),
                List.of(
                        new TestCase(UUID.randomUUID(), 0, "[1]", "1", true),
                        new TestCase(UUID.randomUUID(), 1, "[2]", "2", true),
                        new TestCase(UUID.randomUUID(), 2, "[3]", "3", false),
                        new TestCase(UUID.randomUUID(), 3, "[4]", "4", false)),
                Set.of(),
                true);
    }
}
