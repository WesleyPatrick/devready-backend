package br.com.devready.application.career.exception;

import br.com.devready.application.shared.exception.ApplicationException;
import java.util.List;

public class UnknownCompanyException extends ApplicationException {
    public UnknownCompanyException(List<String> slugs) {
        super("Empresa desconhecida: " + String.join(", ", slugs) + ".");
    }
}
