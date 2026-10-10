package br.com.devready.api.session.dto;

import br.com.devready.api.practice.dto.CompetencyRefResponse;
import br.com.devready.application.session.dto.SessionView;
import br.com.devready.domain.session.Session;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SessionResponse(
        UUID id,
        String format,
        String status,
        AreaRefResponse area,
        String level,
        CompetencyRefResponse competency,
        Instant startedAt,
        Instant deadlineAt,
        List<SessionQuestionResponse> questions,
        List<SessionChallengeResponse> challenges) {

    public static SessionResponse from(SessionView view) {
        Session s = view.session();
        return new SessionResponse(
                s.id(),
                s.format().name(),
                s.status().name(),
                new AreaRefResponse(view.area().slug(), view.area().name()),
                s.level().name(),
                CompetencyRefResponse.from(view.competency()),
                s.startedAt(),
                s.deadlineAt(),
                view.questions().stream().map(SessionQuestionResponse::from).toList(),
                view.challenges().stream().map(SessionChallengeResponse::from).toList());
    }
}
