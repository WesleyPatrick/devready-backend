package br.com.devready.api.career.dto;

import br.com.devready.domain.career.Stack;

public record StackOptionResponse(String slug, String name, String area) {
    public static StackOptionResponse from(Stack stack) {
        return new StackOptionResponse(stack.slug(), stack.name(), stack.areaSlug());
    }
}
