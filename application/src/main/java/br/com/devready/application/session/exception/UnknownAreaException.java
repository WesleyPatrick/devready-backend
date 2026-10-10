package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class UnknownAreaException extends ApplicationException {
    public UnknownAreaException(String slug) {
        super("Área desconhecida: " + slug + ".");
    }
}
