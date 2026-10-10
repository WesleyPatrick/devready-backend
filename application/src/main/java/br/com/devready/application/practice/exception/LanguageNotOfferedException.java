package br.com.devready.application.practice.exception;

import br.com.devready.application.shared.exception.ApplicationException;
import br.com.devready.domain.catalog.Language;

public class LanguageNotOfferedException extends ApplicationException {
    public LanguageNotOfferedException(Language language) {
        super("Este desafio não tem a linguagem " + language.label() + ".");
    }
}
