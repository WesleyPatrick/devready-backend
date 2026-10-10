package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class EmailAlreadyRegisteredException extends ApplicationException {
    public EmailAlreadyRegisteredException(String email) {
        super("Já existe uma conta com o e-mail " + email + ".");
    }
}
