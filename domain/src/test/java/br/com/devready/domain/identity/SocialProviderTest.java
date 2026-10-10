package br.com.devready.domain.identity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class SocialProviderTest {

    @Test
    void eachProviderHasTheNameThePersonReads() {
        assertThat(Arrays.stream(SocialProvider.values()).map(SocialProvider::label))
                .containsExactly("Google", "GitHub");
    }
}
