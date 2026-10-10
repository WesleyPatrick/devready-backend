package br.com.devready.domain.identity;

public class WeakPasswordException extends IdentityException {
    public WeakPasswordException() {
        super("A senha precisa ter pelo menos " + PasswordPolicy.MIN_LENGTH + " caracteres.");
    }

    public WeakPasswordException(String message) {
        super(message);
    }
}
