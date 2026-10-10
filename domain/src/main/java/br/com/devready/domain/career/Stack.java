package br.com.devready.domain.career;

import java.util.UUID;

public record Stack(UUID id, String slug, String name, String areaSlug) {
    public Stack {
        if (id == null) {
            throw new CareerException("Stack sem id.");
        }
        Names.slug(slug, "stack");
        name = Names.text(name, "o nome da stack");
        Names.slug(areaSlug, "área da stack " + slug);
    }
}
