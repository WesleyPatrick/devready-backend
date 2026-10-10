package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class SessionNotFoundException extends ApplicationException {
    public SessionNotFoundException() {
        super("Sessão não encontrada.");
    }
}
