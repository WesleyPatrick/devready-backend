package br.com.devready.infrastructure.identity.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashesWithSaltAndVerifies() {
        String hash = hasher.hash("senha-forte");

        assertThat(hash).startsWith("$2").isNotEqualTo("senha-forte");
        assertThat(hasher.hash("senha-forte")).isNotEqualTo(hash);
        assertThat(hasher.matches("senha-forte", hash)).isTrue();
        assertThat(hasher.matches("outra", hash)).isFalse();
    }

    @Test
    void passwordLongerThanSeventyTwoBytesNeverMatchesInsteadOfThrowing() {
        String hash = hasher.hash("senha-forte");

        assertThat(hasher.matches("ç".repeat(72), hash)).isFalse();
    }

    @Test
    void dummyMatchRunsBcryptWithTheSameCostAsARealHash() {
        long start = System.nanoTime();
        hasher.dummyMatch("qualquer-senha");
        long dummy = System.nanoTime() - start;

        String hash = hasher.hash("senha-forte");
        start = System.nanoTime();
        hasher.matches("qualquer-senha", hash);
        long real = System.nanoTime() - start;

        assertThat(BCryptPasswordHasher.DUMMY_HASH).startsWith("$2a$12$");
        assertThat(dummy).isGreaterThan(real / 4);
    }
}
