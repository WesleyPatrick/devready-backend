package br.com.devready.application.career.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class CareerProfileNotFoundException extends ApplicationException {
    public CareerProfileNotFoundException() {
        super("Perfil de carreira ainda não preenchido.");
    }
}
