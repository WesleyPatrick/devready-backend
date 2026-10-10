package br.com.devready.api.identity.dto;

import br.com.devready.domain.identity.Role;

public record ChangeUserAccessRequest(Boolean active, Role role) {}
