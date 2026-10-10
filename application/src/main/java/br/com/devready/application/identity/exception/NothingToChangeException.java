package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class NothingToChangeException extends ApplicationException {
    public NothingToChangeException() {
        super("Informe o que mudar: a situação da conta, o papel ou os dois.");
    }
}
