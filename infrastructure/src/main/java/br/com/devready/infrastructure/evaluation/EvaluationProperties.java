package br.com.devready.infrastructure.evaluation;

import br.com.devready.domain.catalog.Language;
import java.util.EnumMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "devready.evaluation")
public record EvaluationProperties(Map<Language, String> runtimes) {

    static final Map<Language, String> DEFAULT_RUNTIMES =
            Map.of(
                    Language.JAVA, "Java 13",
                    Language.PYTHON, "Python 3.8",
                    Language.JAVASCRIPT, "Node 12");

    public EvaluationProperties {
        Map<Language, String> merged = new EnumMap<>(DEFAULT_RUNTIMES);
        if (runtimes != null) {
            merged.putAll(runtimes);
        }
        runtimes = Map.copyOf(merged);
    }
}
