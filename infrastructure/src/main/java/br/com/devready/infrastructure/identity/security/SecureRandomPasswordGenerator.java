package br.com.devready.infrastructure.identity.security;

import br.com.devready.application.identity.port.PasswordGenerator;
import java.security.SecureRandom;

public class SecureRandomPasswordGenerator implements PasswordGenerator {

    static final int LENGTH = 16;
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        String password;
        do {
            password = draw();
        } while (!hasEveryKind(password));
        return password;
    }

    private String draw() {
        StringBuilder password = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            password.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return password.toString();
    }

    private static boolean hasEveryKind(String password) {
        return password.chars().anyMatch(Character::isUpperCase)
                && password.chars().anyMatch(Character::isLowerCase)
                && password.chars().anyMatch(Character::isDigit);
    }
}
