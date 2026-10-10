package br.com.devready.api.session.dto;

import br.com.devready.api.practice.dto.CompetencyRefResponse;
import br.com.devready.application.session.dto.SessionSummaryView;
import br.com.devready.domain.session.Session;
import java.time.Instant;
import java.util.UUID;

public record SessionSummaryResponse(
        UUID id,
        String format,
        AreaRefResponse area,
        String level,
        CompetencyRefResponse competency,
        String status,
        Double finalScore,
        int xpAwarded,
        Instant startedAt,
        Instant finishedAt) {

    public static SessionSummaryResponse from(SessionSummaryView view) {
        Session s = view.session();
        return new SessionSummaryResponse(
                s.id(),
                s.format().name(),
                new AreaRefResponse(view.area().slug(), view.area().name()),
                s.level().name(),
                CompetencyRefResponse.from(view.competency()),
                s.status().name(),
                s.finalScore(),
                s.xpAwarded(),
                s.startedAt(),
                s.finishedAt());
    }
}
