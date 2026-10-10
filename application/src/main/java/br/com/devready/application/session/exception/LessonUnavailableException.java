package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class LessonUnavailableException extends ApplicationException {
    public LessonUnavailableException(String competencyName) {
        super(competencyName + " não tem questões suficientes para uma lição.");
    }
}
