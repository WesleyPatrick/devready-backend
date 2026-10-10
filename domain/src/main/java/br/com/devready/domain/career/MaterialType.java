package br.com.devready.domain.career;

public enum MaterialType {
    DOCUMENTACAO("documentacao"),
    LIVRO("livro"),
    CURSO("curso"),
    ARTIGO("artigo"),
    VIDEO("video");

    private final String slug;

    MaterialType(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static MaterialType fromSlug(String slug) {
        for (MaterialType type : values()) {
            if (type.slug.equals(slug)) {
                return type;
            }
        }
        throw new CareerException("Tipo de material desconhecido: " + slug);
    }
}
