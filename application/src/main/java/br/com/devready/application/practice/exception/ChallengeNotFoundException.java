package br.com.devready.application.practice.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class ChallengeNotFoundException extends ApplicationException {
    public ChallengeNotFoundException(String slug) {
        super("Desafio não encontrado: " + slug + ".");
    }
}
