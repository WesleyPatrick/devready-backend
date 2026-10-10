package br.com.devready.application.identity.port;

public interface PasswordHasher {
    String hash(String rawPassword);

    boolean matches(String rawPassword, String passwordHash);

    void dummyMatch(String rawPassword);
}
