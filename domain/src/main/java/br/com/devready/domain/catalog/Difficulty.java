package br.com.devready.domain.catalog;

public enum Difficulty {
    EASY("easy"),
    MEDIUM("medium"),
    HARD("hard");

    private final String slug;

    Difficulty(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static Difficulty fromSlug(String slug) {
        for (Difficulty difficulty : values()) {
            if (difficulty.slug.equals(slug)) {
                return difficulty;
            }
        }
        throw new CatalogException("Dificuldade desconhecida: " + slug);
    }
}
