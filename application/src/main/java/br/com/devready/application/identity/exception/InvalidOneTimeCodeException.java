package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class InvalidOneTimeCodeException extends ApplicationException {
    public InvalidOneTimeCodeException() {
        super("Código de login inválido ou expirado. Tente entrar de novo.");
    }
}
