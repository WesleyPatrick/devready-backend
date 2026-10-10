package br.com.devready.domain.catalog;

import java.util.UUID;

public record Area(UUID id, String slug, String name, SessionConfig sessionConfig, boolean active) {
    public Area {
        Slug.require(slug, "área");
        Text.require(name, "nome da área");
        if (sessionConfig == null) {
            throw new CatalogException("Área precisa de configuração de sessão.");
        }
    }
}
