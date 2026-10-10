package br.com.devready.application.session.dto;

import br.com.devready.domain.session.Session;
import java.util.List;

public record SessionResultView(
        Session session,
        List<CompetencyResultView> competencies,
        List<QuestionCorrectionView> questions,
        List<ChallengeCorrectionView> challenges) {
    public SessionResultView {
        competencies = List.copyOf(competencies);
        questions = List.copyOf(questions);
        challenges = List.copyOf(challenges);
    }
}
