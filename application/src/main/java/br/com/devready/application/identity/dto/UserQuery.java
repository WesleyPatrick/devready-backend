package br.com.devready.application.identity.dto;

import br.com.devready.domain.identity.Role;

public record UserQuery(String text, Role role, Boolean active, int page, int size) {
    public UserFilter filter() {
        return new UserFilter(text, role, active);
    }
}
