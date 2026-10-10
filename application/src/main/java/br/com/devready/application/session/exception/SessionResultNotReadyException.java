package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class SessionResultNotReadyException extends ApplicationException {
    public SessionResultNotReadyException() {
        super("O resultado sai quando a correção terminar.");
    }
}
