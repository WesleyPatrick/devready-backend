package br.com.devready.domain.career;

public enum CompetencyCategory {
    FUNDAMENTO("fundamento"),
    LINGUAGEM("linguagem"),
    FRAMEWORK("framework"),
    PRATICA("pratica"),
    COMPORTAMENTAL("comportamental");

    private final String slug;

    CompetencyCategory(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static CompetencyCategory fromSlug(String slug) {
        for (CompetencyCategory category : values()) {
            if (category.slug.equals(slug)) {
                return category;
            }
        }
        throw new CareerException("Categoria de competência desconhecida: " + slug);
    }
}
