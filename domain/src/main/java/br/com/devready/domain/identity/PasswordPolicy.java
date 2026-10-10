package br.com.devready.domain.identity;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {}

    public static void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.strip().length() < MIN_LENGTH) {
            throw new WeakPasswordException();
        }
        if (rawPassword.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new WeakPasswordException(
                    "A senha pode ter no máximo "
                            + MAX_BYTES
                            + " bytes; caracteres acentuados contam mais de um.");
        }
    }
}
