package br.com.devready.application.identity.fake;

import br.com.devready.application.identity.port.PasswordGenerator;

public class FakePasswordGenerator implements PasswordGenerator {
    private String next = "Provisoria123456";
    private int calls;

    @Override
    public String generate() {
        calls++;
        return next;
    }

    public void willGenerate(String password) {
        this.next = password;
    }

    public int calls() {
        return calls;
    }
}
