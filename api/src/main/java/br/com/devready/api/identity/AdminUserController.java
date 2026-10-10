package br.com.devready.api.identity;

import br.com.devready.api.identity.dto.AdminUserPageResponse;
import br.com.devready.api.identity.dto.AdminUserResponse;
import br.com.devready.api.identity.dto.ChangeUserAccessRequest;
import br.com.devready.api.identity.dto.TemporaryPasswordResponse;
import br.com.devready.application.identity.dto.ChangeUserAccessCommand;
import br.com.devready.application.identity.dto.UserQuery;
import br.com.devready.application.identity.usecase.ChangeUserAccessUseCase;
import br.com.devready.application.identity.usecase.ListUsersUseCase;
import br.com.devready.application.identity.usecase.ResetUserPasswordUseCase;
import br.com.devready.domain.identity.Role;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final ListUsersUseCase listUsers;
    private final ChangeUserAccessUseCase changeUserAccess;
    private final ResetUserPasswordUseCase resetUserPassword;

    public AdminUserController(
            ListUsersUseCase listUsers,
            ChangeUserAccessUseCase changeUserAccess,
            ResetUserPasswordUseCase resetUserPassword) {
        this.listUsers = listUsers;
        this.changeUserAccess = changeUserAccess;
        this.resetUserPassword = resetUserPassword;
    }

    @GetMapping
    public AdminUserPageResponse list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "role", required = false) Role role,
            @RequestParam(name = "active", required = false) Boolean active,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return AdminUserPageResponse.from(
                listUsers.execute(
                        AuthenticatedUser.id(jwt), new UserQuery(q, role, active, page, size)));
    }

    @PatchMapping("/{id}")
    public AdminUserResponse change(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @RequestBody ChangeUserAccessRequest request) {
        return AdminUserResponse.from(
                changeUserAccess.execute(
                        AuthenticatedUser.id(jwt),
                        id,
                        new ChangeUserAccessCommand(request.active(), request.role())));
    }

    @PostMapping("/{id}/password-reset")
    public ResponseEntity<TemporaryPasswordResponse> resetPassword(
            @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        String temporary = resetUserPassword.execute(AuthenticatedUser.id(jwt), id);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(new TemporaryPasswordResponse(temporary));
    }
}
