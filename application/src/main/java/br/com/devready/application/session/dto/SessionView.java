package br.com.devready.application.session.dto;

import br.com.devready.domain.career.Competency;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.session.Session;
import java.util.List;

public record SessionView(
        Session session,
        Area area,
        Competency competency,
        List<SessionQuestionView> questions,
        List<SessionChallengeView> challenges) {
    public SessionView {
        questions = List.copyOf(questions);
        challenges = List.copyOf(challenges);
    }
}
