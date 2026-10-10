package br.com.devready.application.evaluation.fake;

import br.com.devready.application.evaluation.port.HarnessBuilder;
import br.com.devready.domain.catalog.Language;
import java.util.ArrayList;
import java.util.List;

public class FakeHarnessBuilder implements HarnessBuilder {
    private final List<String> functions = new ArrayList<>();

    @Override
    public String build(Language language, String sourceCode, String functionName, String marker) {
        functions.add(functionName);
        return marker + "\n" + language + "\n" + functionName + "\n" + sourceCode;
    }

    public List<String> functions() {
        return List.copyOf(functions);
    }
}
