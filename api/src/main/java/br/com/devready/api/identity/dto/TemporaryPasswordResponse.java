package br.com.devready.api.identity.dto;

public record TemporaryPasswordResponse(String temporaryPassword) {

    @Override
    public String toString() {
        return "TemporaryPasswordResponse[temporaryPassword=<oculta>]";
    }
}
