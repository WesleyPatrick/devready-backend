package br.com.devready.application.identity.dto;

import br.com.devready.domain.identity.Role;

public record UserFilter(String text, Role role, Boolean active) {
    public UserFilter {
        text = text == null || text.isBlank() ? null : text.strip();
    }
}
