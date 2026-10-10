package br.com.devready.api.identity;

import br.com.devready.api.config.FrontendProperties;
import br.com.devready.application.identity.exception.UserInactiveException;
import br.com.devready.application.identity.usecase.LoginWithSocialAccountUseCase;
import br.com.devready.application.shared.exception.ApplicationException;
import br.com.devready.domain.identity.IdentityException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final LoginWithSocialAccountUseCase loginWithSocialAccount;
    private final OAuth2AuthorizedClientService authorizedClients;
    private final GitHubEmailResolver gitHubEmails;
    private final FrontendProperties frontend;

    public OAuth2LoginSuccessHandler(
            LoginWithSocialAccountUseCase loginWithSocialAccount,
            OAuth2AuthorizedClientService authorizedClients,
            GitHubEmailResolver gitHubEmails,
            FrontendProperties frontend) {
        this.loginWithSocialAccount = loginWithSocialAccount;
        this.authorizedClients = authorizedClients;
        this.gitHubEmails = gitHubEmails;
        this.frontend = frontend;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String registrationId = token.getAuthorizedClientRegistrationId();
        try {
            var command =
                    SocialProfileExtractor.extract(
                            registrationId,
                            token.getPrincipal().getAttributes(),
                            () -> gitHubEmail(registrationId, token.getName()));
            String code = loginWithSocialAccount.execute(command).value();
            invalidate(request);
            response.sendRedirect(frontend.frontendUrl() + "/auth/callback?code=" + code);
        } catch (UserInactiveException e) {
            invalidate(request);
            response.sendRedirect(frontend.frontendUrl() + "/login?error=inactive");
        } catch (ApplicationException
                | IdentityException
                | DataIntegrityViolationException
                | RestClientException
                | IllegalArgumentException e) {
            log.warn("Login social falhou para o provedor {}: {}", registrationId, e.getMessage());
            invalidate(request);
            response.sendRedirect(frontend.frontendUrl() + "/login?error=social");
        } finally {
            authorizedClients.removeAuthorizedClient(registrationId, token.getName());
        }
    }

    private Optional<String> gitHubEmail(String registrationId, String principalName) {
        if (!"github".equals(registrationId)) {
            return Optional.empty();
        }
        OAuth2AuthorizedClient client =
                authorizedClients.loadAuthorizedClient(registrationId, principalName);
        if (client == null) {
            return Optional.empty();
        }
        return gitHubEmails.primaryEmail(client.getAccessToken().getTokenValue());
    }

    private static void invalidate(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
