package br.com.devready.domain.catalog;

import java.util.UUID;

public record Tag(UUID id, String slug, String name) {
    public Tag {
        Slug.require(slug, "tag");
        Text.require(name, "nome da tag");
    }
}
