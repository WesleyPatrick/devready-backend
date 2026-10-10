package br.com.devready.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.devready.api.config.FrontendProperties;
import br.com.devready.application.identity.dto.OneTimeCode;
import br.com.devready.application.identity.dto.SocialLoginCommand;
import br.com.devready.application.identity.exception.UserInactiveException;
import br.com.devready.application.identity.usecase.LoginWithSocialAccountUseCase;
import br.com.devready.domain.identity.IdentityException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class OAuth2LoginSuccessHandlerTest {

    private final LoginWithSocialAccountUseCase useCase = mock(LoginWithSocialAccountUseCase.class);
    private final OAuth2AuthorizedClientService clients = mock(OAuth2AuthorizedClientService.class);
    private final OAuth2LoginSuccessHandler handler =
            new OAuth2LoginSuccessHandler(
                    useCase,
                    clients,
                    token -> Optional.of("gh@example.com"),
                    new FrontendProperties("http://front"));

    private static OAuth2AuthenticationToken google() {
        var user =
                new DefaultOAuth2User(
                        List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                        Map.of(
                                "sub",
                                "g-1",
                                "email",
                                "ana@example.com",
                                "email_verified",
                                true,
                                "name",
                                "Ana"),
                        "sub");
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
    }

    private static OAuth2AuthenticationToken github() {
        var user =
                new DefaultOAuth2User(
                        List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                        Map.of("id", 7, "login", "anadev"),
                        "id");
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "github");
    }

    private MockHttpServletResponse run(OAuth2AuthenticationToken token, MockHttpSession session)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(session);
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, token);
        return response;
    }

    @Test
    void redirectsWithCodeAndForgetsTheProviderTokens() throws Exception {
        when(useCase.execute(any())).thenReturn(new OneTimeCode("abc"));
        MockHttpSession session = new MockHttpSession();

        MockHttpServletResponse response = run(google(), session);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://front/auth/callback?code=abc");
        assertThat(session.isInvalid()).isTrue();
        verify(clients).removeAuthorizedClient("google", "g-1");
    }

    @Test
    void readsGitHubEmailWithTheStoredTokenAndThenRemovesIt() throws Exception {
        when(useCase.execute(any())).thenReturn(new OneTimeCode("xyz"));
        when(clients.loadAuthorizedClient("github", "7")).thenReturn(gitHubClient());

        MockHttpServletResponse response = run(github(), new MockHttpSession());

        ArgumentCaptor<SocialLoginCommand> command =
                ArgumentCaptor.forClass(SocialLoginCommand.class);
        verify(useCase).execute(command.capture());
        assertThat(command.getValue().email()).isEqualTo("gh@example.com");
        assertThat(response.getRedirectedUrl()).isEqualTo("http://front/auth/callback?code=xyz");
        verify(clients).removeAuthorizedClient("github", "7");
    }

    @Test
    void domainRuleFromTheProviderDataSendsBackToLogin() throws Exception {
        when(useCase.execute(any()))
                .thenThrow(new IdentityException("Já existe uma conta Google vinculada."));

        MockHttpServletResponse response = run(google(), new MockHttpSession());

        assertThat(response.getRedirectedUrl()).isEqualTo("http://front/login?error=social");
        verify(clients).removeAuthorizedClient("google", "g-1");
    }

    @Test
    void concurrentInsertSendsBackToLogin() throws Exception {
        when(useCase.execute(any())).thenThrow(new DataIntegrityViolationException("unique"));

        MockHttpServletResponse response = run(google(), new MockHttpSession());

        assertThat(response.getRedirectedUrl()).isEqualTo("http://front/login?error=social");
        verify(clients).removeAuthorizedClient("google", "g-1");
    }

    @Test
    void inactiveUserGoesToLoginWithInactiveError() throws Exception {
        when(useCase.execute(any())).thenThrow(new UserInactiveException());

        MockHttpServletResponse response = run(google(), new MockHttpSession());

        assertThat(response.getRedirectedUrl()).isEqualTo("http://front/login?error=inactive");
        verify(clients).removeAuthorizedClient("google", "g-1");
    }

    private static OAuth2AuthorizedClient gitHubClient() {
        ClientRegistration registration =
                ClientRegistration.withRegistrationId("github")
                        .clientId("id")
                        .clientSecret("secret")
                        .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                        .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                        .authorizationUri("https://github.com/login/oauth/authorize")
                        .tokenUri("https://github.com/login/oauth/access_token")
                        .build();
        var token =
                new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER,
                        "gh-token",
                        Instant.EPOCH,
                        Instant.EPOCH.plusSeconds(3600));
        return new OAuth2AuthorizedClient(registration, "7", token);
    }
}
