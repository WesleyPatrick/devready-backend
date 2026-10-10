package br.com.devready.api.config;

import br.com.devready.api.identity.OAuth2LoginFailureHandler;
import br.com.devready.api.identity.OAuth2LoginSuccessHandler;
import br.com.devready.infrastructure.identity.security.JwtSecretKey;
import jakarta.servlet.DispatcherType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties({FrontendProperties.class, SocialLoginProperties.class})
public class SecurityConfig {

    static final String[] CREDENTIAL_ROUTES = {
        "/api/v1/auth/register",
        "/api/v1/auth/login",
        "/api/v1/auth/refresh",
        "/api/v1/auth/exchange",
        "/api/v1/auth/logout"
    };

    static final String[] PUBLIC_GET_ROUTES = {"/api/v1/career/options", "/api/v1/auth/providers"};

    @Bean
    SocialClientRegistrations socialClientRegistrations(SocialLoginProperties properties) {
        return SocialClientRegistrations.from(properties);
    }

    @Bean
    JwtDecoder jwtDecoder(JwtSecretKey key) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withSecretKey(key.value())
                        .macAlgorithm(MacAlgorithm.HS256)
                        .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("devready"));
        return decoder;
    }

    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    BearerTokenResolver bearerTokenResolver() {
        DefaultBearerTokenResolver header = new DefaultBearerTokenResolver();
        List<RequestMatcher> matchers = new ArrayList<>();
        for (String path : CREDENTIAL_ROUTES) {
            matchers.add(PathPatternRequestMatcher.withDefaults().matcher(path));
        }
        for (String path : PUBLIC_GET_ROUTES) {
            matchers.add(PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, path));
        }
        RequestMatcher withoutBearer = new OrRequestMatcher(matchers);
        return request -> withoutBearer.matches(request) ? null : header.resolve(request);
    }

    @Bean
    @Order(1)
    SecurityFilterChain oauth2LoginFilterChain(
            HttpSecurity http,
            OAuth2LoginSuccessHandler successHandler,
            OAuth2LoginFailureHandler failureHandler)
            throws Exception {
        http.securityMatcher("/oauth2/**", "/login/oauth2/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .oauth2Login(
                        login ->
                                login.successHandler(successHandler)
                                        .failureHandler(failureHandler)
                                        .withObjectPostProcessor(
                                                new ObjectPostProcessor<
                                                        OAuth2AuthorizationRequestRedirectFilter>() {
                                                    @Override
                                                    public <
                                                                    O extends
                                                                            OAuth2AuthorizationRequestRedirectFilter>
                                                            O postProcess(O filter) {
                                                        filter.setAuthenticationFailureHandler(
                                                                failureHandler);
                                                        return filter;
                                                    }
                                                }));
        return http.build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain apiSecurityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter converter,
            BearerTokenResolver bearerTokenResolver,
            ProblemResponseWriter problems)
            throws Exception {
        var entryPoint = new ProblemAuthenticationEntryPoint(problems);
        var accessDenied = new ProblemAccessDeniedHandler(problems);
        http.csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        handling ->
                                handling.authenticationEntryPoint(entryPoint)
                                        .accessDeniedHandler(accessDenied))
                .authorizeHttpRequests(
                        requests ->
                                requests.dispatcherTypeMatchers(DispatcherType.ERROR)
                                        .permitAll()
                                        .requestMatchers("/actuator/health/**")
                                        .permitAll()
                                        .requestMatchers(
                                                "/api/docs/**",
                                                "/api/swagger-ui/**",
                                                "/api/swagger-ui.html")
                                        .permitAll()
                                        .requestMatchers(CREDENTIAL_ROUTES)
                                        .permitAll()
                                        .requestMatchers(HttpMethod.GET, PUBLIC_GET_ROUTES)
                                        .permitAll()
                                        .requestMatchers("/api/v1/admin/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .oauth2ResourceServer(
                        resource ->
                                resource.bearerTokenResolver(bearerTokenResolver)
                                        .authenticationEntryPoint(entryPoint)
                                        .accessDeniedHandler(accessDenied)
                                        .jwt(
                                                jwt ->
                                                        jwt.decoder(jwtDecoder)
                                                                .jwtAuthenticationConverter(
                                                                        converter)))
                .addFilterAfter(
                        new PasswordChangeRequiredFilter(problems),
                        BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
