package br.com.devready.api.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.devready.api.IntegrationTest;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.domain.identity.SocialProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

class OAuth2LoginTest extends IntegrationTest {

    @Autowired private OAuth2LoginSuccessHandler successHandler;
    @Autowired private UserRepository users;

    @Test
    void authorizationEndpointRedirectsToGoogleAndStartsASession() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(
                        header().string(
                                        "Location",
                                        org.hamcrest.Matchers.startsWith(
                                                "https://accounts.google.com/o/oauth2/v2/auth")))
                .andExpect(header().exists("Set-Cookie"));
    }

    @Test
    void successHandlerCreatesUserAndRedirectsWithCodeThatExchangesForTokens() throws Exception {
        String email = "social-" + UUID.randomUUID() + "@example.com";
        var principal =
                new DefaultOAuth2User(
                        List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                        Map.of(
                                "sub",
                                "g-" + email,
                                "email",
                                email,
                                "email_verified",
                                true,
                                "name",
                                "Ana Social",
                                "picture",
                                "https://img/a.png"),
                        "sub");
        var authentication =
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authentication);

        String location = response.getRedirectedUrl();
        assertThat(location).startsWith("http://localhost:5173/auth/callback?code=");
        String code = location.substring(location.indexOf("code=") + 5);
        assertThat(users.findByEmail(email)).isPresent();
        assertThat(users.findByEmail(email).orElseThrow().hasSocialAccount(SocialProvider.GOOGLE))
                .isTrue();

        mockMvc.perform(
                        post("/api/v1/auth/exchange")
                                .contentType(APPLICATION_JSON)
                                .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(email))
                .andExpect(jsonPath("$.user.hasPassword").value(false))
                .andExpect(jsonPath("$.user.providers[0]").value("GOOGLE"));
        mockMvc.perform(
                        post("/api/v1/auth/exchange")
                                .contentType(APPLICATION_JSON)
                                .content("{\"code\":\"" + code + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void successHandlerSendsInactiveUsersBackToLoginWithError() throws Exception {
        String email = "inactive-" + UUID.randomUUID() + "@example.com";
        var principal =
                new DefaultOAuth2User(
                        List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
                        Map.of(
                                "sub",
                                "g-" + email,
                                "email",
                                email,
                                "email_verified",
                                true,
                                "name",
                                "X"),
                        "sub");
        var authentication =
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "google");
        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(), new MockHttpServletResponse(), authentication);
        var user = users.findByEmail(email).orElseThrow();
        user.deactivate();
        users.save(user);

        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/login?error=inactive");
    }

    @Test
    void successHandlerSendsGitHubUsersWithoutEmailBackToLoginWithError() throws Exception {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", 4242);
        attributes.put("login", "sem-email");
        attributes.put("avatar_url", "https://img/gh.png");
        attributes.put("email", null);
        var principal =
                new DefaultOAuth2User(
                        List.of(new SimpleGrantedAuthority("OAUTH2_USER")), attributes, "id");
        var authentication =
                new OAuth2AuthenticationToken(principal, principal.getAuthorities(), "github");

        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(
                new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/login?error=social");
    }
}
