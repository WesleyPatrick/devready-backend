package br.com.devready.api.identity.dto;

import br.com.devready.application.identity.dto.UserPage;
import java.util.List;

public record AdminUserPageResponse(
        List<AdminUserResponse> items, int page, int totalPages, int totalItems) {
    public static AdminUserPageResponse from(UserPage page) {
        return new AdminUserPageResponse(
                page.items().stream().map(AdminUserResponse::from).toList(),
                page.page(),
                page.totalPages(),
                page.totalItems());
    }
}
