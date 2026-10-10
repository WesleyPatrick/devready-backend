package br.com.devready.api.identity;

import br.com.devready.api.config.SocialClientRegistrations;
import br.com.devready.api.identity.dto.AuthProvidersResponse;
import br.com.devready.api.identity.dto.AuthTokensResponse;
import br.com.devready.api.identity.dto.ExchangeRequest;
import br.com.devready.api.identity.dto.LoginRequest;
import br.com.devready.api.identity.dto.RefreshRequest;
import br.com.devready.api.identity.dto.RegisterRequest;
import br.com.devready.api.identity.dto.UserResponse;
import br.com.devready.application.identity.dto.RegisterUserCommand;
import br.com.devready.application.identity.usecase.ExchangeOneTimeCodeUseCase;
import br.com.devready.application.identity.usecase.GetCurrentUserUseCase;
import br.com.devready.application.identity.usecase.LoginWithPasswordUseCase;
import br.com.devready.application.identity.usecase.LogoutAllUseCase;
import br.com.devready.application.identity.usecase.LogoutUseCase;
import br.com.devready.application.identity.usecase.RefreshSessionUseCase;
import br.com.devready.application.identity.usecase.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final RegisterUserUseCase registerUser;
    private final LoginWithPasswordUseCase loginWithPassword;
    private final RefreshSessionUseCase refreshSession;
    private final LogoutUseCase logout;
    private final LogoutAllUseCase logoutAll;
    private final ExchangeOneTimeCodeUseCase exchangeOneTimeCode;
    private final GetCurrentUserUseCase getCurrentUser;
    private final SocialClientRegistrations socialRegistrations;

    public AuthController(
            RegisterUserUseCase registerUser,
            LoginWithPasswordUseCase loginWithPassword,
            RefreshSessionUseCase refreshSession,
            LogoutUseCase logout,
            LogoutAllUseCase logoutAll,
            ExchangeOneTimeCodeUseCase exchangeOneTimeCode,
            GetCurrentUserUseCase getCurrentUser,
            SocialClientRegistrations socialRegistrations) {
        this.registerUser = registerUser;
        this.loginWithPassword = loginWithPassword;
        this.refreshSession = refreshSession;
        this.logout = logout;
        this.logoutAll = logoutAll;
        this.exchangeOneTimeCode = exchangeOneTimeCode;
        this.getCurrentUser = getCurrentUser;
        this.socialRegistrations = socialRegistrations;
    }

    @GetMapping("/providers")
    @SecurityRequirements
    public AuthProvidersResponse providers() {
        return new AuthProvidersResponse(socialRegistrations.providers());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    public AuthTokensResponse register(@Valid @RequestBody RegisterRequest request) {
        return AuthTokensResponse.from(
                registerUser.execute(
                        new RegisterUserCommand(
                                request.email(), request.password(), request.displayName())));
    }

    @PostMapping("/login")
    @SecurityRequirements
    public AuthTokensResponse login(@Valid @RequestBody LoginRequest request) {
        return AuthTokensResponse.from(
                loginWithPassword.execute(request.email(), request.password()));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    public AuthTokensResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return AuthTokensResponse.from(refreshSession.execute(request.refreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    public void logout(@Valid @RequestBody RefreshRequest request) {
        logout.execute(request.refreshToken());
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal Jwt jwt) {
        logoutAll.execute(AuthenticatedUser.id(jwt));
    }

    @PostMapping("/exchange")
    @SecurityRequirements
    public AuthTokensResponse exchange(@Valid @RequestBody ExchangeRequest request) {
        return AuthTokensResponse.from(exchangeOneTimeCode.execute(request.code()));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return UserResponse.from(getCurrentUser.execute(AuthenticatedUser.id(jwt)));
    }
}
