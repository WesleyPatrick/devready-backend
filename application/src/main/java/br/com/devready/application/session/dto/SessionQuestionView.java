package br.com.devready.application.session.dto;

import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionQuestion;
import java.util.UUID;

public record SessionQuestionView(
        int position, Question question, UUID chosenOptionId, Boolean correct, boolean revealed) {

    public static SessionQuestionView of(Session session, SessionQuestion item, Question question) {
        boolean revealed = session.reveals(item);
        return new SessionQuestionView(
                item.position(), question, item.optionId(), revealed ? item.hit() : null, revealed);
    }
}
