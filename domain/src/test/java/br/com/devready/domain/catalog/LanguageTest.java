package br.com.devready.domain.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class LanguageTest {

    @Test
    void eachLanguageHasTheNameThePersonReads() {
        assertThat(Arrays.stream(Language.values()).map(Language::label))
                .containsExactly("Java", "Python", "JavaScript");
    }
}
