package br.com.devready.api.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final List<String> LIMITED_PATHS =
            List.of(
                    "/api/v1/auth/login",
                    "/api/v1/auth/register",
                    "/api/v1/challenges/*/runs",
                    "/api/v1/challenges/*/submissions",
                    "/api/v1/sessions/*/challenges/*/runs",
                    "/api/v1/sessions/*/challenges/*/submissions",
                    "/api/v1/admin/users/*/password-reset");

    private static final Map<String, RequestMatcher> LIMITED_ROUTES =
            LIMITED_PATHS.stream()
                    .collect(
                            Collectors.toUnmodifiableMap(
                                    path -> path,
                                    path ->
                                            PathPatternRequestMatcher.withDefaults()
                                                    .matcher(HttpMethod.POST, path)));

    private final RateLimitProperties properties;
    private final ProblemResponseWriter problems;
    private final Cache<String, Bucket> buckets =
            Caffeine.newBuilder()
                    .maximumSize(10_000)
                    .expireAfterAccess(Duration.ofMinutes(10))
                    .build();

    public RateLimitFilter(RateLimitProperties properties, ProblemResponseWriter problems) {
        this.properties = properties;
        this.problems = problems;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || limitedRoute(request) == null;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String key = clientKey(request) + " " + limitedRoute(request);
        Bucket bucket = buckets.get(key, ignored -> newBucket());
        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
            return;
        }
        problems.write(
                response,
                Problems.of(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Muitas tentativas",
                        "Aguarde um minuto antes de tentar de novo."));
    }

    private static String limitedRoute(HttpServletRequest request) {
        for (String path : LIMITED_PATHS) {
            if (LIMITED_ROUTES.get(path).matches(request)) {
                return path;
            }
        }
        return null;
    }

    private Bucket newBucket() {
        Bandwidth limit =
                Bandwidth.builder()
                        .capacity(properties.requestsPerMinute())
                        .refillGreedy(properties.requestsPerMinute(), Duration.ofMinutes(1))
                        .build();
        return Bucket.builder().addLimit(limit).build();
    }

    private static String clientKey(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
