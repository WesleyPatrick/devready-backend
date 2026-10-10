package br.com.devready.application.practice.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class OptionNotInQuestionException extends ApplicationException {
    public OptionNotInQuestionException() {
        super("A alternativa escolhida não é desta questão.");
    }
}
