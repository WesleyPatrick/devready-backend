package br.com.devready.domain.catalog;

public enum Language {
    JAVA("Java"),
    PYTHON("Python"),
    JAVASCRIPT("JavaScript");

    private final String label;

    Language(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
