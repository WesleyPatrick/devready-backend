package br.com.devready.application.practice.dto;

import br.com.devready.domain.career.Competency;
import java.util.List;

public record Placement(Competency competency, List<String> askedBy) {
    public Placement {
        askedBy = List.copyOf(askedBy);
    }
}
