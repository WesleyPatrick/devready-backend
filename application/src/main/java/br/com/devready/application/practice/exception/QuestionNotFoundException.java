package br.com.devready.application.practice.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class QuestionNotFoundException extends ApplicationException {
    public QuestionNotFoundException(String slug) {
        super("Questão não encontrada: " + slug + ".");
    }
}
