package br.com.devready.application.identity.exception;

public class InvalidCurrentPasswordException extends InvalidCredentialsException {

    public static final String FIELD = "currentPassword";

    private InvalidCurrentPasswordException(String message) {
        super(message);
    }

    public static InvalidCurrentPasswordException missing() {
        return new InvalidCurrentPasswordException("Informe a senha atual.");
    }

    public static InvalidCurrentPasswordException incorrect() {
        return new InvalidCurrentPasswordException("Senha atual incorreta.");
    }
}
