package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class ItemNotInSessionException extends ApplicationException {
    public ItemNotInSessionException(String what, int position) {
        super("Não existe " + what + " " + position + " nesta sessão.");
    }
}
