package br.com.devready.domain.career;

public enum CompanyGroup {
    BIG_TECH("big-tech"),
    TECH_BRASIL("tech-brasil");

    private final String slug;

    CompanyGroup(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static CompanyGroup fromSlug(String slug) {
        for (CompanyGroup group : values()) {
            if (group.slug.equals(slug)) {
                return group;
            }
        }
        throw new CareerException("Grupo de empresa desconhecido: " + slug);
    }
}
