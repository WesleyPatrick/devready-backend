package br.com.devready.api.career.dto;

import br.com.devready.domain.career.Material;

public record MaterialResponse(
        String title, String url, String type, String language, boolean free) {
    public static MaterialResponse from(Material material) {
        return new MaterialResponse(
                material.title(),
                material.url(),
                material.type().slug(),
                material.language(),
                material.free());
    }
}
