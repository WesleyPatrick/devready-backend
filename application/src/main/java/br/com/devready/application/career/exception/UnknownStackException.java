package br.com.devready.application.career.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class UnknownStackException extends ApplicationException {
    public UnknownStackException(String slug) {
        super("Stack desconhecida: " + slug + ".");
    }
}
