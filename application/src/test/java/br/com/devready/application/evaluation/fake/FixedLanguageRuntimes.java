package br.com.devready.application.evaluation.fake;

import br.com.devready.application.evaluation.port.LanguageRuntimes;
import br.com.devready.domain.catalog.Language;
import java.util.Map;

public class FixedLanguageRuntimes implements LanguageRuntimes {
    private static final Map<Language, String> LABELS =
            Map.of(
                    Language.JAVA, "Java 13",
                    Language.PYTHON, "Python 3.8",
                    Language.JAVASCRIPT, "Node 12");

    @Override
    public String labelOf(Language language) {
        return LABELS.get(language);
    }
}
