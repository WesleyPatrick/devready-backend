package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class InvalidRefreshTokenException extends ApplicationException {
    public InvalidRefreshTokenException() {
        super("Sessão inválida ou expirada. Entre de novo.");
    }
}
