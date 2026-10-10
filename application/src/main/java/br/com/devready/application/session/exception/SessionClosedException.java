package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class SessionClosedException extends ApplicationException {
    public SessionClosedException() {
        super("Esta sessão não aceita mais respostas nem envios.");
    }
}
