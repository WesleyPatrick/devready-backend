package br.com.devready.infrastructure.identity.redis;

import br.com.devready.application.identity.port.RefreshTokenStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.redis.core.StringRedisTemplate;

public class RedisRefreshTokenStore implements RefreshTokenStore {

    private static final String ACTIVE = "refresh:";
    private static final String USED = "refresh:used:";
    private static final String BY_USER = "refresh:user:";

    private final StringRedisTemplate redis;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public RedisRefreshTokenStore(StringRedisTemplate redis, Duration ttl) {
        this.redis = redis;
        this.ttl = ttl;
    }

    @Override
    public String issue(UUID userId) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set(ACTIVE + token, userId.toString(), ttl);
        redis.opsForSet().add(BY_USER + userId, token);
        redis.expire(BY_USER + userId, ttl);
        return token;
    }

    @Override
    public ConsumeResult consume(String token) {
        String owner = redis.opsForValue().getAndDelete(ACTIVE + token);
        if (owner != null) {
            redis.opsForSet().remove(BY_USER + owner, token);
            redis.opsForValue().set(USED + token, owner, ttl);
            return ConsumeResult.valid(UUID.fromString(owner));
        }
        String reusedBy = redis.opsForValue().get(USED + token);
        if (reusedBy != null) {
            return ConsumeResult.reused(UUID.fromString(reusedBy));
        }
        return ConsumeResult.unknown();
    }

    @Override
    public void revoke(String token) {
        String owner = redis.opsForValue().getAndDelete(ACTIVE + token);
        if (owner != null) {
            redis.opsForSet().remove(BY_USER + owner, token);
        }
    }

    @Override
    public void revokeAll(UUID userId) {
        Set<String> tokens = redis.opsForSet().members(BY_USER + userId);
        if (tokens != null) {
            tokens.forEach(token -> redis.delete(ACTIVE + token));
        }
        redis.delete(BY_USER + userId);
    }
}
