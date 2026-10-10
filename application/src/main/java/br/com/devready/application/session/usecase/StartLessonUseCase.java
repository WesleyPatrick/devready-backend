package br.com.devready.application.session.usecase;

import br.com.devready.application.career.dto.LearningTrackView;
import br.com.devready.application.career.dto.TrackCompetencyView;
import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.career.exception.CompetencyNotInTrackException;
import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.application.session.exception.LessonUnavailableException;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.application.session.service.SessionOpener;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.domain.career.CareerProfile;
import br.com.devready.domain.career.Competency;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.practice.Answer;
import br.com.devready.domain.session.RandomSource;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionAssembly;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StartLessonUseCase {

    private final CareerProfileRepository profiles;
    private final TrackAssembler assembler;
    private final AreaRepository areas;
    private final SessionPool pool;
    private final AnswerRepository answers;
    private final SimulationRecordReader simulations;
    private final SessionOpener opener;
    private final RandomSource random;
    private final IdGenerator ids;
    private final Clock clock;

    public StartLessonUseCase(
            CareerProfileRepository profiles,
            TrackAssembler assembler,
            AreaRepository areas,
            SessionPool pool,
            AnswerRepository answers,
            SimulationRecordReader simulations,
            SessionOpener opener,
            RandomSource random,
            IdGenerator ids,
            Clock clock) {
        this.profiles = profiles;
        this.assembler = assembler;
        this.areas = areas;
        this.pool = pool;
        this.answers = answers;
        this.simulations = simulations;
        this.opener = opener;
        this.random = random;
        this.ids = ids;
        this.clock = clock;
    }

    public Session execute(UUID userId, String competencySlug) {
        CareerProfile profile =
                profiles.findByUserId(userId).orElseThrow(CareerProfileNotFoundException::new);
        LearningTrackView track = assembler.assemble(userId);
        Competency competency =
                track.competencies().stream()
                        .map(TrackCompetencyView::competency)
                        .filter(c -> c.slug().equals(competencySlug))
                        .findFirst()
                        .orElseThrow(() -> new CompetencyNotInTrackException(competencySlug));
        Area area =
                areas.findBySlug(track.stack().areaSlug())
                        .orElseThrow(() -> new LessonUnavailableException(competency.name()));
        List<UUID> candidates =
                competency.tagSlugs().isEmpty()
                        ? List.of()
                        : pool
                                .activeQuestionsTagged(
                                        area.id(), competency.tagSlugs(), track.visibleLevels())
                                .stream()
                                .map(Question::id)
                                .toList();
        Set<UUID> solved = new HashSet<>(simulations.questionsSolvedBy(userId));
        answers.findByUser(userId).stream()
                .filter(Answer::correct)
                .map(Answer::questionId)
                .forEach(solved::add);
        List<UUID> picked = SessionAssembly.lesson(candidates, solved, random);
        if (picked.isEmpty()) {
            throw new LessonUnavailableException(competency.name());
        }
        return opener.open(
                Session.lesson(
                        ids.next(),
                        userId,
                        area.id(),
                        profile.seniority(),
                        competencySlug,
                        picked,
                        clock.now()));
    }
}
