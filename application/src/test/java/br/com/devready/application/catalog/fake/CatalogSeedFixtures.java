package br.com.devready.application.catalog.fake;

import br.com.devready.application.catalog.dto.AreaSeed;
import br.com.devready.application.catalog.dto.CatalogSeed;
import br.com.devready.application.catalog.dto.ChallengeSeed;
import br.com.devready.application.catalog.dto.QuestionSeed;
import br.com.devready.application.catalog.dto.TagSeed;
import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.Level;
import java.util.List;
import java.util.Map;

public final class CatalogSeedFixtures {

    private CatalogSeedFixtures() {}

    public static AreaSeed backend() {
        return new AreaSeed("backend", "Backend", 8, 2, 45, 0.5);
    }

    public static TagSeed http() {
        return new TagSeed("http", "HTTP");
    }

    public static QuestionSeed idempotency() {
        return new QuestionSeed(
                "backend-junior-http-idempotency",
                "Idempotência dos métodos HTTP",
                "backend",
                Level.JUNIOR,
                Difficulty.EASY,
                "Qual método HTTP é idempotente por definição?",
                "PUT substitui o recurso inteiro.",
                List.of(
                        new QuestionSeed.OptionSeed("POST", false),
                        new QuestionSeed.OptionSeed("PUT", true),
                        new QuestionSeed.OptionSeed("PATCH", false)),
                List.of("http"));
    }

    public static ChallengeSeed twoSum() {
        return new ChallengeSeed(
                "two-sum",
                "Soma de dois números",
                "backend",
                Level.JUNIOR,
                Difficulty.EASY,
                "Dado um array de inteiros e um alvo...",
                "Mapa de valor para índice.",
                5000,
                262144,
                ComparisonMode.UNORDERED,
                5,
                Map.of(
                        Language.JAVA,
                        new ChallengeSeed.LanguageSeed(
                                "twoSum",
                                "class Solution { public int[] twoSum(int[] nums, int target) { return null; } }",
                                "class Solution { public int[] twoSum(int[] nums, int target) { return new int[]{0, 1}; } }")),
                List.of(
                        new ChallengeSeed.TestCaseSeed("[[2,7,11,15],9]", "[0,1]", true),
                        new ChallengeSeed.TestCaseSeed("[[3,2,4],6]", "[1,2]", true),
                        new ChallengeSeed.TestCaseSeed("[[3,3],6]", "[0,1]", false),
                        new ChallengeSeed.TestCaseSeed("[[1,5,9],14]", "[1,2]", false)),
                List.of("http"));
    }

    public static CatalogSeed seed() {
        return new CatalogSeed(
                List.of(backend()), List.of(http()), List.of(idempotency()), List.of(twoSum()));
    }
}
