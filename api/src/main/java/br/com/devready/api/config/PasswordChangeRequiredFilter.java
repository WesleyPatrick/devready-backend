package br.com.devready.api.config;

import br.com.devready.infrastructure.identity.security.NimbusTokenIssuer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    public static final String CODE = "password_change_required";

    private static final RequestMatcher ALLOWED =
            new OrRequestMatcher(
                    List.of(
                            route(HttpMethod.GET, "/api/v1/auth/me"),
                            route(HttpMethod.PATCH, "/api/v1/me"),
                            route(HttpMethod.POST, "/api/v1/auth/refresh"),
                            route(HttpMethod.POST, "/api/v1/auth/logout"),
                            route(HttpMethod.POST, "/api/v1/auth/logout-all")));

    private final ProblemResponseWriter problems;

    public PasswordChangeRequiredFilter(ProblemResponseWriter problems) {
        this.problems = problems;
    }

    private static RequestMatcher route(HttpMethod method, String path) {
        return PathPatternRequestMatcher.withDefaults().matcher(method, path);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (mustChangePassword() && !ALLOWED.matches(request)) {
            ProblemDetail problem =
                    Problems.of(
                            HttpStatus.FORBIDDEN,
                            "Senha provisória",
                            "Defina uma senha nova para continuar.");
            problem.setProperty("code", CODE);
            problems.write(response, problem);
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean mustChangePassword() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof JwtAuthenticationToken jwt
                && Boolean.TRUE.equals(
                        jwt.getToken().getClaim(NimbusTokenIssuer.PASSWORD_CHANGE_CLAIM));
    }
}
