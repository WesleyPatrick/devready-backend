package br.com.devready.application.identity.fake;

import br.com.devready.application.identity.port.PasswordHasher;

public class FakePasswordHasher implements PasswordHasher {
    private int dummyMatches = 0;

    @Override
    public String hash(String rawPassword) {
        return "hashed:" + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String passwordHash) {
        return ("hashed:" + rawPassword).equals(passwordHash);
    }

    @Override
    public void dummyMatch(String rawPassword) {
        dummyMatches++;
    }

    public int dummyMatches() {
        return dummyMatches;
    }
}
