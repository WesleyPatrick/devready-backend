package br.com.devready.infrastructure.evaluation;

import br.com.devready.application.catalog.dto.ChallengeSeed;
import br.com.devready.domain.catalog.ComparisonMode;
import br.com.devready.domain.catalog.Language;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.infrastructure.catalog.seed.YamlCatalogSeedSource;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public final class SeedChallenges {

    public record Reference(
            String slug,
            Language language,
            String functionName,
            String code,
            List<TestCase> cases,
            ComparisonMode mode) {
        @Override
        public String toString() {
            return slug + " em " + language;
        }
    }

    private SeedChallenges() {}

    public static List<Reference> references() {
        return of(ChallengeSeed.LanguageSeed::referenceSolution);
    }

    public static List<Reference> starters() {
        return of(ChallengeSeed.LanguageSeed::starterCode);
    }

    private static List<Reference> of(Function<ChallengeSeed.LanguageSeed, String> code) {
        List<Reference> all = new ArrayList<>();
        for (ChallengeSeed challenge :
                new YamlCatalogSeedSource("classpath:seed").load().challenges()) {
            List<TestCase> cases = new ArrayList<>();
            for (int i = 0; i < challenge.testCases().size(); i++) {
                ChallengeSeed.TestCaseSeed testCase = challenge.testCases().get(i);
                cases.add(
                        new TestCase(
                                UUID.randomUUID(),
                                i,
                                testCase.argsJson(),
                                testCase.expectedJson(),
                                testCase.visible()));
            }
            for (Language language : List.of(Language.JAVA, Language.PYTHON, Language.JAVASCRIPT)) {
                ChallengeSeed.LanguageSeed entry = challenge.languages().get(language);
                all.add(
                        new Reference(
                                challenge.slug(),
                                language,
                                entry.functionName(),
                                code.apply(entry),
                                List.copyOf(cases),
                                challenge.comparison()));
            }
        }
        return all;
    }
}
