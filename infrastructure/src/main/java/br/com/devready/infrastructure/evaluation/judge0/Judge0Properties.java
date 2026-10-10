package br.com.devready.infrastructure.evaluation.judge0;

import br.com.devready.domain.catalog.Language;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devready.judge0")
public record Judge0Properties(
        URI url,
        Duration pollInterval,
        Duration pollTimeout,
        Map<Language, LanguageSettings> languages) {

    public record LanguageSettings(int id, long memoryLimitKb) {}

    public Judge0Properties {
        if (url == null) {
            throw new IllegalStateException("Configure devready.judge0.url.");
        }
        pollInterval = pollInterval == null ? Duration.ofMillis(500) : pollInterval;
        pollTimeout = pollTimeout == null ? Duration.ofSeconds(60) : pollTimeout;
        languages = languages == null ? Map.of() : Map.copyOf(languages);
    }

    public LanguageSettings settingsOf(Language language) {
        LanguageSettings settings = languages.get(language);
        if (settings == null) {
            throw new IllegalStateException("Linguagem sem configuração do Judge0: " + language);
        }
        return settings;
    }

    public int maxPolls() {
        return (int) Math.max(1, pollTimeout.toMillis() / Math.max(1, pollInterval.toMillis()));
    }
}
