package br.com.devready.api.identity;

import br.com.devready.api.identity.dto.UpdateProfileRequest;
import br.com.devready.api.identity.dto.UserResponse;
import br.com.devready.application.identity.dto.UpdateProfileCommand;
import br.com.devready.application.identity.usecase.UpdateProfileUseCase;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UpdateProfileUseCase updateProfile;

    public MeController(UpdateProfileUseCase updateProfile) {
        this.updateProfile = updateProfile;
    }

    @PatchMapping
    public UserResponse update(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody UpdateProfileRequest request) {
        return UserResponse.from(
                updateProfile.execute(
                        new UpdateProfileCommand(
                                AuthenticatedUser.id(jwt),
                                request.displayName(),
                                request.avatarUrl(),
                                request.newPassword(),
                                request.currentPassword())));
    }
}
