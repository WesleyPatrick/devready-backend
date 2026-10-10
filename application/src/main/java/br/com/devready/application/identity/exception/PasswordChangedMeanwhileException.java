package br.com.devready.application.identity.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class PasswordChangedMeanwhileException extends ApplicationException {
    public PasswordChangedMeanwhileException() {
        super("Sua senha foi alterada por outro caminho. Entre de novo.");
    }
}
