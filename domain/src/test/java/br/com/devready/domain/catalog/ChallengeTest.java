package br.com.devready.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChallengeTest {

    private static TestCase tc(int position, boolean visible) {
        return new TestCase(UUID.randomUUID(), position, "[[2,7,11,15],9]", "[0,1]", visible);
    }

    private static Map<Language, ChallengeLanguage> java() {
        return Map.of(
                Language.JAVA,
                new ChallengeLanguage(
                        Language.JAVA,
                        "twoSum",
                        "class Solution { public int[] twoSum(int[] nums, int target) { return null; } }",
                        "class Solution { public int[] twoSum(int[] nums, int target) { return new int[]{0,1}; } }"));
    }

    private static Challenge challenge(
            List<TestCase> cases, Map<Language, ChallengeLanguage> languages) {
        return challenge(cases, languages, 5);
    }

    private static Challenge challenge(
            List<TestCase> cases, Map<Language, ChallengeLanguage> languages, int maxSubmissions) {
        return new Challenge(
                UUID.randomUUID(),
                "two-sum",
                "Soma de dois números",
                UUID.randomUUID(),
                Level.JUNIOR,
                Difficulty.EASY,
                "Dado um array...",
                "Use um mapa.",
                new ExecutionLimits(5000, 262144),
                ComparisonMode.UNORDERED,
                maxSubmissions,
                languages,
                cases,
                Set.of("arrays"),
                true);
    }

    private static final List<TestCase> FOUR =
            List.of(tc(0, true), tc(1, true), tc(2, false), tc(3, false));

    @Test
    void splitsVisibleAndHiddenCases() {
        Challenge c = challenge(FOUR, java());

        assertThat(c.visibleTestCases()).hasSize(2);
        assertThat(c.hiddenTestCases()).hasSize(2);
        assertThat(c.languages()).containsKey(Language.JAVA);
    }

    @Test
    void requiresTwoVisibleAndTwoHiddenCases() {
        assertThatThrownBy(
                        () -> challenge(List.of(tc(0, true), tc(1, false), tc(2, false)), java()))
                .isInstanceOf(InvalidChallengeException.class);
        assertThatThrownBy(() -> challenge(List.of(tc(0, true), tc(1, true), tc(2, false)), java()))
                .isInstanceOf(InvalidChallengeException.class);
    }

    @Test
    void requiresALanguageUniquePositionsAndSaneLimits() {
        assertThatThrownBy(() -> challenge(FOUR, Map.of()))
                .isInstanceOf(InvalidChallengeException.class);
        assertThatThrownBy(
                        () ->
                                challenge(
                                        List.of(
                                                tc(0, true),
                                                tc(0, true),
                                                tc(2, false),
                                                tc(3, false)),
                                        java()))
                .isInstanceOf(InvalidChallengeException.class);
        assertThatThrownBy(() -> new ExecutionLimits(0, 1000)).isInstanceOf(CatalogException.class);
        assertThatThrownBy(() -> new ChallengeLanguage(Language.JAVA, " ", "code", null))
                .isInstanceOf(CatalogException.class);
        assertThatThrownBy(
                        () ->
                                new Challenge(
                                        UUID.randomUUID(),
                                        "two-sum",
                                        "T",
                                        UUID.randomUUID(),
                                        Level.JUNIOR,
                                        Difficulty.EASY,
                                        "s",
                                        "a",
                                        new ExecutionLimits(5000, 262144),
                                        ComparisonMode.EXACT,
                                        0,
                                        java(),
                                        FOUR,
                                        Set.of(),
                                        true))
                .isInstanceOf(InvalidChallengeException.class);
    }

    @Test
    void theSubmissionLimitGoesFromOneToFive() {
        assertThat(challenge(FOUR, java(), 1).maxSubmissions()).isEqualTo(1);
        assertThat(challenge(FOUR, java(), 5).maxSubmissions()).isEqualTo(5);
        assertThatThrownBy(() -> challenge(FOUR, java(), 0))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessage("O limite de envios do desafio precisa ficar entre 1 e 5.");
        assertThatThrownBy(() -> challenge(FOUR, java(), 6))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessage("O limite de envios do desafio precisa ficar entre 1 e 5.");
    }

    @Test
    void languageKeyMustMatchTheEntry() {
        Map<Language, ChallengeLanguage> mismatched =
                Map.of(Language.PYTHON, java().get(Language.JAVA));
        assertThatThrownBy(() -> challenge(FOUR, mismatched))
                .isInstanceOf(InvalidChallengeException.class);
    }

    @Test
    void requiresDifficulty() {
        assertThatThrownBy(
                        () ->
                                new Challenge(
                                        UUID.randomUUID(),
                                        "two-sum",
                                        "Soma de dois números",
                                        UUID.randomUUID(),
                                        Level.JUNIOR,
                                        null,
                                        "Dado um array...",
                                        "Use um mapa.",
                                        new ExecutionLimits(5000, 262144),
                                        ComparisonMode.UNORDERED,
                                        5,
                                        java(),
                                        FOUR,
                                        Set.of(),
                                        true))
                .isInstanceOf(InvalidChallengeException.class)
                .hasMessageContaining("dificuldade");
    }
}
