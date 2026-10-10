package br.com.devready.application.practice.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class SubmissionNotFoundException extends ApplicationException {
    public SubmissionNotFoundException() {
        super("Envio não encontrado.");
    }
}
