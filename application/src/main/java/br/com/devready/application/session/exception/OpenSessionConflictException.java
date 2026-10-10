package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class OpenSessionConflictException extends ApplicationException {
    public OpenSessionConflictException() {
        super("Outra sessão começou ao mesmo tempo.");
    }
}
