package br.com.devready.infrastructure.identity.security;

import br.com.devready.application.identity.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptPasswordHasher implements PasswordHasher {

    static final String DUMMY_HASH = "$2a$12$qiw64aOL7Q9fWXibjSRGrOs9eR0yW8PJHLrU7juyZha3kjxg4sOSS";

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return encoder.matches(rawPassword, passwordHash);
    }

    @Override
    public void dummyMatch(String rawPassword) {
        encoder.matches(rawPassword, DUMMY_HASH);
    }
}
