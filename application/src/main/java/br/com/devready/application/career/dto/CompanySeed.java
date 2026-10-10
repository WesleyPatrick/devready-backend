package br.com.devready.application.career.dto;

import java.util.List;

public record CompanySeed(
        String slug, String name, String group, String about, List<String> careers) {}
