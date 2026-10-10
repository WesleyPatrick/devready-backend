package br.com.devready.application.evaluation.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class XpAlreadyGrantedException extends ApplicationException {
    public XpAlreadyGrantedException() {
        super("A pessoa já ganhou o XP deste desafio.");
    }
}
