package br.com.devready.domain.identity;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {

    @Test
    void acceptsEightCharactersOrMore() {
        assertThatCode(() -> PasswordPolicy.validate("abcdefgh")).doesNotThrowAnyException();
    }

    @Test
    void rejectsShortPasswords() {
        assertThatThrownBy(() -> PasswordPolicy.validate("abcdefg"))
                .isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void rejectsNullAndBlank() {
        assertThatThrownBy(() -> PasswordPolicy.validate(null))
                .isInstanceOf(WeakPasswordException.class);
        assertThatThrownBy(() -> PasswordPolicy.validate("        "))
                .isInstanceOf(WeakPasswordException.class);
    }

    @Test
    void acceptsSeventyTwoBytes() {
        assertThatCode(() -> PasswordPolicy.validate("a".repeat(72))).doesNotThrowAnyException();
        assertThatCode(() -> PasswordPolicy.validate("ç".repeat(36))).doesNotThrowAnyException();
    }

    @Test
    void rejectsMoreThanSeventyTwoBytesEvenWithSeventyTwoCharacters() {
        assertThatThrownBy(() -> PasswordPolicy.validate("ç".repeat(72)))
                .isInstanceOf(WeakPasswordException.class)
                .hasMessage(
                        "A senha pode ter no máximo 72 bytes; caracteres acentuados contam mais"
                                + " de um.");
    }
}
