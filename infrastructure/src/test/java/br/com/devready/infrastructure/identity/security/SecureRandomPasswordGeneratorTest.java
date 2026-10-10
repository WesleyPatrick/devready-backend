package br.com.devready.infrastructure.identity.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import br.com.devready.domain.identity.PasswordPolicy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class SecureRandomPasswordGeneratorTest {

    private static final int SAMPLES = 500;

    private final SecureRandomPasswordGenerator generator = new SecureRandomPasswordGenerator();

    @Test
    void everyPasswordHasSixteenLettersAndDigitsWithEachKindAndPassesThePolicy() {
        List<String> passwords =
                IntStream.range(0, SAMPLES).mapToObj(i -> generator.generate()).toList();

        for (String password : passwords) {
            assertThat(password).hasSize(16).matches("[A-Za-z0-9]{16}");
            assertThat(password).containsPattern("[A-Z]");
            assertThat(password).containsPattern("[a-z]");
            assertThat(password).containsPattern("[0-9]");
            assertThatCode(() -> PasswordPolicy.validate(password)).doesNotThrowAnyException();
        }
    }

    @Test
    void passwordsDoNotRepeatAndUseTheWholeAlphabet() {
        Set<String> distinct = new HashSet<>();
        Set<Character> used = new HashSet<>();
        for (int i = 0; i < SAMPLES; i++) {
            String password = generator.generate();
            distinct.add(password);
            password.chars().forEach(c -> used.add((char) c));
        }

        assertThat(distinct).hasSize(SAMPLES);
        assertThat(used).hasSize(62);
    }
}
