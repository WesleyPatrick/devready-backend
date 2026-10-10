package br.com.devready.infrastructure.identity.redis;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.devready.application.identity.port.OneTimeCodeStore;
import br.com.devready.application.identity.port.RefreshTokenStore;
import br.com.devready.infrastructure.InfrastructureTestApplication;
import br.com.devready.infrastructure.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = InfrastructureTestApplication.class)
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
class RedisStoresTest {

    @Autowired private RefreshTokenStore refreshTokens;
    @Autowired private OneTimeCodeStore oneTimeCodes;
    @Autowired private StringRedisTemplate redis;

    @Test
    void refreshTokenIsSingleUseAndReuseIsDetected() {
        UUID userId = UUID.randomUUID();
        String token = refreshTokens.issue(userId);

        assertThat(redis.getExpire("refresh:" + token)).isBetween(600_000L, 604_800L);
        assertThat(refreshTokens.consume(token))
                .isEqualTo(RefreshTokenStore.ConsumeResult.valid(userId));
        assertThat(refreshTokens.consume(token))
                .isEqualTo(RefreshTokenStore.ConsumeResult.reused(userId));
        assertThat(refreshTokens.consume("nunca-existiu"))
                .isEqualTo(RefreshTokenStore.ConsumeResult.unknown());
    }

    @Test
    void revokeAllDropsEveryTokenOfTheUserOnly() {
        UUID ana = UUID.randomUUID();
        UUID bob = UUID.randomUUID();
        String a1 = refreshTokens.issue(ana);
        String a2 = refreshTokens.issue(ana);
        String b1 = refreshTokens.issue(bob);

        refreshTokens.revokeAll(ana);

        assertThat(refreshTokens.consume(a1).status()).isEqualTo(RefreshTokenStore.Status.UNKNOWN);
        assertThat(refreshTokens.consume(a2).status()).isEqualTo(RefreshTokenStore.Status.UNKNOWN);
        assertThat(refreshTokens.consume(b1).status()).isEqualTo(RefreshTokenStore.Status.VALID);
        assertThat(redis.hasKey("refresh:user:" + ana)).isFalse();
    }

    @Test
    void revokeDropsOneToken() {
        UUID ana = UUID.randomUUID();
        String token = refreshTokens.issue(ana);

        refreshTokens.revoke(token);

        assertThat(refreshTokens.consume(token).status())
                .isEqualTo(RefreshTokenStore.Status.UNKNOWN);
        assertThat(redis.opsForSet().members("refresh:user:" + ana)).isEmpty();
    }

    @Test
    void oneTimeCodeIsConsumedOnceAndExpires() {
        UUID userId = UUID.randomUUID();
        String code = oneTimeCodes.issue(userId);

        assertThat(redis.getExpire("oauth-code:" + code)).isBetween(30L, 60L);
        assertThat(oneTimeCodes.consume(code)).contains(userId);
        assertThat(oneTimeCodes.consume(code)).isEmpty();
    }
}
