package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class CannotChangeOwnAccountException extends ApplicationException {
    public CannotChangeOwnAccountException() {
        super("Você não pode alterar a sua própria conta por aqui.");
    }
}
