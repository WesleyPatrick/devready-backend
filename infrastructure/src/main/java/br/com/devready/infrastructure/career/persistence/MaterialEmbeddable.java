package br.com.devready.infrastructure.career.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class MaterialEmbeddable {
    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 2)
    private String language;

    @Column(nullable = false)
    private boolean free;

    protected MaterialEmbeddable() {}

    public MaterialEmbeddable(
            String title, String url, String type, String language, boolean free) {
        this.title = title;
        this.url = url;
        this.type = type;
        this.language = language;
        this.free = free;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getType() {
        return type;
    }

    public String getLanguage() {
        return language;
    }

    public boolean isFree() {
        return free;
    }
}
