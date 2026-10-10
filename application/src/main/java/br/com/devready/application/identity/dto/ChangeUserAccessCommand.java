package br.com.devready.application.identity.dto;

import br.com.devready.domain.identity.Role;

public record ChangeUserAccessCommand(Boolean active, Role role) {
    public boolean isEmpty() {
        return active == null && role == null;
    }
}
