package br.com.devready.api.catalog.dto;

import br.com.devready.application.catalog.dto.AreaView;
import br.com.devready.domain.catalog.Area;
import java.util.List;

public record AreaResponse(
        String slug,
        String name,
        int questionCount,
        int challengeCount,
        int durationMinutes,
        double objectiveWeight,
        List<String> levels) {
    public static AreaResponse from(AreaView view) {
        Area area = view.area();
        return new AreaResponse(
                area.slug(),
                area.name(),
                area.sessionConfig().questionCount(),
                area.sessionConfig().challengeCount(),
                area.sessionConfig().durationMinutes(),
                area.sessionConfig().objectiveWeight(),
                view.levels().stream().map(Enum::name).toList());
    }
}
