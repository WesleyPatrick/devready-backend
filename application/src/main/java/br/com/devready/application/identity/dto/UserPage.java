package br.com.devready.application.identity.dto;

import br.com.devready.domain.identity.User;
import java.util.List;

public record UserPage(List<User> items, int page, int totalPages, int totalItems) {}
