package br.com.devready.infrastructure.config;

import br.com.devready.application.identity.port.OneTimeCodeStore;
import br.com.devready.application.identity.port.PasswordGenerator;
import br.com.devready.application.identity.port.PasswordHasher;
import br.com.devready.application.identity.port.RefreshTokenStore;
import br.com.devready.application.identity.port.TokenIssuer;
import br.com.devready.application.identity.port.UserRepository;
import br.com.devready.application.identity.service.IssueSession;
import br.com.devready.application.identity.usecase.BootstrapAdminUseCase;
import br.com.devready.application.identity.usecase.ChangeUserAccessUseCase;
import br.com.devready.application.identity.usecase.ExchangeOneTimeCodeUseCase;
import br.com.devready.application.identity.usecase.GetCurrentUserUseCase;
import br.com.devready.application.identity.usecase.ListUsersUseCase;
import br.com.devready.application.identity.usecase.LoginWithPasswordUseCase;
import br.com.devready.application.identity.usecase.LoginWithSocialAccountUseCase;
import br.com.devready.application.identity.usecase.LogoutAllUseCase;
import br.com.devready.application.identity.usecase.LogoutUseCase;
import br.com.devready.application.identity.usecase.RefreshSessionUseCase;
import br.com.devready.application.identity.usecase.RegisterUserUseCase;
import br.com.devready.application.identity.usecase.ResetUserPasswordUseCase;
import br.com.devready.application.identity.usecase.UpdateProfileUseCase;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.application.shared.Transactions;
import br.com.devready.infrastructure.identity.redis.RedisOneTimeCodeStore;
import br.com.devready.infrastructure.identity.redis.RedisRefreshTokenStore;
import br.com.devready.infrastructure.identity.security.BCryptPasswordHasher;
import br.com.devready.infrastructure.identity.security.JwtSecretKey;
import br.com.devready.infrastructure.identity.security.NimbusTokenIssuer;
import br.com.devready.infrastructure.identity.security.SecureRandomPasswordGenerator;
import br.com.devready.infrastructure.shared.RandomIdGenerator;
import br.com.devready.infrastructure.shared.SystemClock;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, AuthProperties.class})
public class IdentityConfiguration {

    @Bean
    Clock clock() {
        return new SystemClock();
    }

    @Bean
    IdGenerator idGenerator() {
        return new RandomIdGenerator();
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    @Bean
    PasswordGenerator passwordGenerator() {
        return new SecureRandomPasswordGenerator();
    }

    @Bean
    JwtSecretKey jwtSecretKey(JwtProperties properties, Environment environment) {
        properties.requireRealSecretOutsideDev(List.of(environment.getActiveProfiles()));
        return new JwtSecretKey(properties);
    }

    @Bean
    TokenIssuer tokenIssuer(JwtSecretKey key, JwtProperties properties, Clock clock) {
        return new NimbusTokenIssuer(key, properties, clock);
    }

    @Bean
    RefreshTokenStore refreshTokenStore(StringRedisTemplate redis, AuthProperties properties) {
        return new RedisRefreshTokenStore(redis, properties.refreshTokenTtl());
    }

    @Bean
    OneTimeCodeStore oneTimeCodeStore(StringRedisTemplate redis, AuthProperties properties) {
        return new RedisOneTimeCodeStore(redis, properties.oneTimeCodeTtl());
    }

    @Bean
    IssueSession issueSession(TokenIssuer tokenIssuer, RefreshTokenStore refreshTokenStore) {
        return new IssueSession(tokenIssuer, refreshTokenStore);
    }

    @Bean
    RegisterUserUseCase registerUser(
            UserRepository users,
            PasswordHasher hasher,
            IssueSession issueSession,
            IdGenerator ids,
            Clock clock) {
        return new RegisterUserUseCase(users, hasher, issueSession, ids, clock);
    }

    @Bean
    LoginWithPasswordUseCase loginWithPassword(
            UserRepository users, PasswordHasher hasher, IssueSession issueSession) {
        return new LoginWithPasswordUseCase(users, hasher, issueSession);
    }

    @Bean
    RefreshSessionUseCase refreshSession(
            UserRepository users, RefreshTokenStore store, IssueSession issueSession) {
        return new RefreshSessionUseCase(users, store, issueSession);
    }

    @Bean
    LogoutUseCase logout(RefreshTokenStore store) {
        return new LogoutUseCase(store);
    }

    @Bean
    LogoutAllUseCase logoutAll(RefreshTokenStore store) {
        return new LogoutAllUseCase(store);
    }

    @Bean
    LoginWithSocialAccountUseCase loginWithSocialAccount(
            UserRepository users, OneTimeCodeStore codes, IdGenerator ids, Clock clock) {
        return new LoginWithSocialAccountUseCase(users, codes, ids, clock);
    }

    @Bean
    ExchangeOneTimeCodeUseCase exchangeOneTimeCode(
            UserRepository users, OneTimeCodeStore codes, IssueSession issueSession) {
        return new ExchangeOneTimeCodeUseCase(users, codes, issueSession);
    }

    @Bean
    GetCurrentUserUseCase getCurrentUser(UserRepository users) {
        return new GetCurrentUserUseCase(users);
    }

    @Bean
    UpdateProfileUseCase updateProfile(
            UserRepository users,
            PasswordHasher hasher,
            RefreshTokenStore refreshTokens,
            Transactions transactions) {
        return new UpdateProfileUseCase(users, hasher, refreshTokens, transactions);
    }

    @Bean
    BootstrapAdminUseCase bootstrapAdmin(
            UserRepository users, PasswordHasher hasher, IdGenerator ids, Clock clock) {
        return new BootstrapAdminUseCase(users, hasher, ids, clock);
    }

    @Bean
    ListUsersUseCase listUsers(UserRepository users) {
        return new ListUsersUseCase(users);
    }

    @Bean
    ChangeUserAccessUseCase changeUserAccess(
            UserRepository users, RefreshTokenStore refreshTokens, Transactions transactions) {
        return new ChangeUserAccessUseCase(users, refreshTokens, transactions);
    }

    @Bean
    ResetUserPasswordUseCase resetUserPassword(
            UserRepository users,
            PasswordHasher hasher,
            PasswordGenerator passwords,
            RefreshTokenStore refreshTokens,
            Transactions transactions) {
        return new ResetUserPasswordUseCase(users, hasher, passwords, refreshTokens, transactions);
    }
}
