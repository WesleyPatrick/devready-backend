package br.com.devready.application.session.exception;

import br.com.devready.application.shared.exception.ApplicationException;

public class NoSubmissionsLeftException extends ApplicationException {
    public NoSubmissionsLeftException(int position, int maxSubmissions) {
        super(message(position, maxSubmissions));
    }

    private static String message(int position, int maxSubmissions) {
        if (maxSubmissions == 1) {
            return "O único envio do desafio " + position + " já foi usado.";
        }
        return "Os " + maxSubmissions + " envios do desafio " + position + " já foram usados.";
    }
}
