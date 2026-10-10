package br.com.devready.application.identity.usecase;

import br.com.devready.application.identity.dto.UserFilter;
import br.com.devready.application.identity.dto.UserPage;
import br.com.devready.application.identity.dto.UserQuery;
import br.com.devready.application.identity.exception.NotAnActiveAdminException;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.domain.identity.User;
import java.util.List;
import java.util.UUID;

public class ListUsersUseCase {

    public static final int MAX_PAGE_SIZE = 50;

    private final UserRepository users;

    public ListUsersUseCase(UserRepository users) {
        this.users = users;
    }

    public UserPage execute(UUID actorId, UserQuery query) {
        users.findById(actorId)
                .filter(User::canManageUsers)
                .orElseThrow(NotAnActiveAdminException::new);
        UserFilter filter = query.filter();
        int total = users.count(filter);
        int size = Math.clamp(query.size(), 1, MAX_PAGE_SIZE);
        int totalPages = Math.max(1, (total + size - 1) / size);
        int page = Math.clamp(query.page(), 1, totalPages);
        List<User> items = total == 0 ? List.of() : users.find(filter, page, size);
        return new UserPage(items, page, totalPages, total);
    }
}
