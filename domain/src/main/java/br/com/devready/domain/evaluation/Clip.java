package br.com.devready.domain.evaluation;

final class Clip {

    static final int MAX_CHARS = 8_000;

    private Clip() {}

    static String of(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String trimmed = text.strip();
        return trimmed.length() <= MAX_CHARS ? trimmed : trimmed.substring(0, MAX_CHARS) + "\n…";
    }
}
