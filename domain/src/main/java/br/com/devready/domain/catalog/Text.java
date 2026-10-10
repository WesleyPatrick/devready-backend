package br.com.devready.domain.catalog;

final class Text {
    private Text() {}

    static String require(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new CatalogException("Campo obrigatório: " + what);
        }
        return value;
    }
}
