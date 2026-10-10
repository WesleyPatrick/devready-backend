package br.com.devready.domain.career;

public enum Requirement {
    OBRIGATORIA("obrigatoria"),
    DESEJAVEL("desejavel");

    private final String slug;

    Requirement(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static Requirement fromSlug(String slug) {
        for (Requirement requirement : values()) {
            if (requirement.slug.equals(slug)) {
                return requirement;
            }
        }
        throw new CareerException(
                "Exigência desconhecida: " + slug + " (use obrigatoria ou desejavel).");
    }
}
