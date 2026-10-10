package br.com.devready.domain.identity;

public class InvalidEmailException extends IdentityException {
    public InvalidEmailException(String email) {
        super("E-mail inválido: " + email);
    }
}
