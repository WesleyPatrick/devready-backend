package br.com.devready.domain.career;

public record Material(String title, String url, MaterialType type, String language, boolean free) {
    public Material {
        title = Names.text(title, "o título do material");
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            throw new CareerException("Material \"" + title + "\" com URL inválida: " + url);
        }
        if (type == null) {
            throw new CareerException("Material \"" + title + "\" sem tipo.");
        }
        if (!"pt".equals(language) && !"en".equals(language)) {
            throw new CareerException(
                    "Material \""
                            + title
                            + "\" com idioma inválido: "
                            + language
                            + " (use pt ou en).");
        }
    }
}
