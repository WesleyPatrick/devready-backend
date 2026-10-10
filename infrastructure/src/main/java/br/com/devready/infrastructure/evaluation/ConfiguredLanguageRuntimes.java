package br.com.devready.infrastructure.evaluation;

import br.com.devready.application.evaluation.port.LanguageRuntimes;
import br.com.devready.domain.catalog.Language;

public class ConfiguredLanguageRuntimes implements LanguageRuntimes {

    private final EvaluationProperties properties;

    public ConfiguredLanguageRuntimes(EvaluationProperties properties) {
        this.properties = properties;
    }

    @Override
    public String labelOf(Language language) {
        return properties.runtimes().get(language);
    }
}
