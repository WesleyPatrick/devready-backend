package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class NotAnActiveAdminException extends ApplicationException {
    public NotAnActiveAdminException() {
        super("Você não tem mais acesso à gestão de usuários.");
    }
}
