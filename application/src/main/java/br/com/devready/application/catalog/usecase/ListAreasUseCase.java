package br.com.devready.application.catalog.usecase;

import br.com.devready.application.catalog.dto.AreaView;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.catalog.port.ChallengeRepository;
import br.com.devready.application.catalog.port.QuestionRepository;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Level;
import java.util.Arrays;
import java.util.List;

public class ListAreasUseCase {

    private final AreaRepository areas;
    private final QuestionRepository questions;
    private final ChallengeRepository challenges;

    public ListAreasUseCase(
            AreaRepository areas, QuestionRepository questions, ChallengeRepository challenges) {
        this.areas = areas;
        this.questions = questions;
        this.challenges = challenges;
    }

    public List<AreaView> execute() {
        return areas.findAllActive().stream()
                .map(area -> new AreaView(area, levelsOf(area)))
                .toList();
    }

    private List<Level> levelsOf(Area area) {
        return Arrays.stream(Level.values())
                .filter(
                        level ->
                                questions.countActiveByAreaAndLevel(area.id(), level)
                                                >= area.sessionConfig().questionCount()
                                        && challenges.countActiveByAreaAndLevel(area.id(), level)
                                                >= area.sessionConfig().challengeCount())
                .toList();
    }
}
