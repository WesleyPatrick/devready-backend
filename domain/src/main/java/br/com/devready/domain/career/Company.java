package br.com.devready.domain.career;

import java.util.List;
import java.util.UUID;

public record Company(
        UUID id,
        String slug,
        String name,
        CompanyGroup group,
        String about,
        List<String> careers,
        boolean active) {
    public Company {
        if (id == null) {
            throw new CareerException("Empresa sem id.");
        }
        Names.slug(slug, "empresa");
        name = Names.text(name, "o nome da empresa");
        if (group == null) {
            throw new CareerException("Empresa " + slug + " sem grupo.");
        }
        about = about == null ? "" : about.strip();
        if (careers == null) {
            careers = List.of();
        } else {
            for (String page : careers) {
                if (page == null || page.isBlank()) {
                    throw new CareerException("Página de carreira vazia na empresa " + slug + ".");
                }
            }
            careers = List.copyOf(careers);
        }
    }
}
