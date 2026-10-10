package br.com.devready.domain.catalog;

import java.util.UUID;

public record TestCase(
        UUID id, int position, String argsJson, String expectedJson, boolean visible) {
    public TestCase {
        Text.require(argsJson, "argumentos do caso de teste");
        Text.require(expectedJson, "saída esperada do caso de teste");
        if (position < 0) {
            throw new CatalogException("Posição negativa em caso de teste.");
        }
    }
}
