package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class NewPasswordSameAsTemporaryException extends ApplicationException {
    public NewPasswordSameAsTemporaryException() {
        super("A senha nova precisa ser diferente da provisória.");
    }
}
