package br.com.devready.domain.career;

import br.com.devready.domain.catalog.Level;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record CareerProfile(
        UUID userId, List<String> companySlugs, String stackSlug, Level seniority) {

    public static final int MAX_COMPANIES = 5;

    public CareerProfile {
        if (userId == null) {
            throw new CareerException("Perfil de carreira sem usuário.");
        }
        if (companySlugs == null || companySlugs.isEmpty()) {
            throw new CareerException("Escolha pelo menos uma empresa.");
        }
        if (companySlugs.size() > MAX_COMPANIES) {
            throw new CareerException("Escolha até " + MAX_COMPANIES + " empresas.");
        }
        Set<String> seen = new HashSet<>();
        for (String slug : companySlugs) {
            Names.slug(slug, "empresa");
            if (!seen.add(slug)) {
                throw new CareerException("Empresa repetida: " + slug);
            }
        }
        companySlugs = List.copyOf(companySlugs);
        if (stackSlug == null || stackSlug.isBlank()) {
            throw new CareerException("Escolha uma stack.");
        }
        Names.slug(stackSlug, "stack");
        if (seniority == null) {
            throw new CareerException("Escolha a senioridade.");
        }
    }
}
