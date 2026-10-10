package br.com.devready.domain.catalog;

public class InvalidQuestionException extends CatalogException {
    public InvalidQuestionException(String message) {
        super(message);
    }
}
