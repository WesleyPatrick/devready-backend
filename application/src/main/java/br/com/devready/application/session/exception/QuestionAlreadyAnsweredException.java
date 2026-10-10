package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class QuestionAlreadyAnsweredException extends ApplicationException {
    public QuestionAlreadyAnsweredException() {
        super("Esta questão da lição já foi respondida.");
    }
}
