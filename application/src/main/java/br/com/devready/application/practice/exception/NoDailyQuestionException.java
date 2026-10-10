package br.com.devready.application.practice.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class NoDailyQuestionException extends ApplicationException {
    public NoDailyQuestionException() {
        super("Sua trilha ainda não tem questão objetiva.");
    }
}
