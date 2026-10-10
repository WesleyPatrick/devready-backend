package br.com.devready.infrastructure.identity.redis;

import br.com.devready.application.identity.port.OneTimeCodeStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisOneTimeCodeStore implements OneTimeCodeStore {

    private static final String PREFIX = "oauth-code:";

    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public RedisOneTimeCodeStore(StringRedisTemplate redis, Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public String issue(UUID userId) {
        byte[] bytes = new byte[24];
        random.nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(PREFIX + code, userId.toString(), ttl);
        return code;
    }

    @Override
    public Optional<UUID> consume(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String userId = redis.opsForValue().getAndDelete(PREFIX + code);
        return Optional.ofNullable(userId).map(UUID::fromString);
    }
}
