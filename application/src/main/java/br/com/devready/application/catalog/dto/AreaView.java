package br.com.devready.application.catalog.dto;

import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Level;
import java.util.List;

public record AreaView(Area area, List<Level> levels) {
    public AreaView {
        levels = List.copyOf(levels);
    }
}
