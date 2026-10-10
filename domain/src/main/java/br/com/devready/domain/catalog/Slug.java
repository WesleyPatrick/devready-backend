package br.com.devready.domain.catalog;

import java.util.regex.Pattern;

final class Slug {
    private static final Pattern VALID = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    private Slug() {}

    static String require(String slug, String what) {
        if (slug == null || !VALID.matcher(slug).matches()) {
            throw new CatalogException("Slug inválido para " + what + ": " + slug);
        }
        return slug;
    }
}
