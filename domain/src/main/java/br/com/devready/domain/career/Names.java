package br.com.devready.domain.career;

import java.util.regex.Pattern;

final class Names {
    private static final Pattern SLUG = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private Names() {}

    static String slug(String value, String what) {
        if (value == null || !SLUG.matcher(value).matches()) {
            throw new CareerException("Slug inválido para " + what + ": " + value);
        }
        return value;
    }

    static String text(String value, String what) {
        if (value == null || value.isBlank()) {
            throw new CareerException("Informe " + what + ".");
        }
        return value.strip();
    }
}
