package br.com.devready.application.practice.dto;

import br.com.devready.application.evaluation.port.LanguageRuntimes;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.ChallengeLanguage;
import br.com.devready.domain.catalog.Language;
import java.util.Arrays;
import java.util.List;

public record ChallengeLanguageView(
        Language language, String runtime, String functionName, String starterCode) {

    public static List<ChallengeLanguageView> listOf(
            Challenge challenge, LanguageRuntimes runtimes) {
        return Arrays.stream(Language.values())
                .filter(challenge.languages()::containsKey)
                .map(
                        language -> {
                            ChallengeLanguage entry = challenge.languages().get(language);
                            return new ChallengeLanguageView(
                                    language,
                                    runtimes.labelOf(language),
                                    entry.functionName(),
                                    entry.starterCode());
                        })
                .toList();
    }
}
