package br.com.devready.application.evaluation.port;

import br.com.devready.domain.catalog.Language;

public interface HarnessBuilder {
    String build(Language language, String sourceCode, String functionName, String marker);
}
