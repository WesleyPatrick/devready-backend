package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class LastActiveAdminException extends ApplicationException {
    public LastActiveAdminException() {
        super("O sistema precisa de ao menos um admin ativo.");
    }
}
