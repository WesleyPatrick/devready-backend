package br.com.devready.application.career.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class CompetencyNotInTrackException extends ApplicationException {
    public CompetencyNotInTrackException(String slug) {
        super("Competência fora da sua trilha: " + slug + ".");
    }
}
