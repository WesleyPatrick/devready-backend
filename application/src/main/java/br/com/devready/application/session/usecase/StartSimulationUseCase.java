package br.com.devready.application.session.usecase;

import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.career.port.CareerProfileRepository;
import br.com.devready.application.catalog.port.AreaRepository;
import br.com.devready.application.session.exception.InsufficientPoolException;
import br.com.devready.application.session.exception.UnknownAreaException;
import br.com.devready.application.session.port.SessionPool;
import br.com.devready.application.session.service.SessionOpener;
import br.com.devready.application.shared.Clock;
import br.com.devready.application.shared.IdGenerator;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import br.com.devready.domain.catalog.SessionConfig;
import br.com.devready.domain.session.RandomSource;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionAssembly;
import java.util.List;
import java.util.UUID;

public class StartSimulationUseCase {

    private final CareerProfileRepository profiles;
    private final AreaRepository areas;
    private final SessionPool pool;
    private final SessionOpener opener;
    private final RandomSource random;
    private final IdGenerator ids;
    private final Clock clock;

    public StartSimulationUseCase(
            CareerProfileRepository profiles,
            AreaRepository areas,
            SessionPool pool,
            SessionOpener opener,
            RandomSource random,
            IdGenerator ids,
            Clock clock) {
        this.profiles = profiles;
        this.areas = areas;
        this.pool = pool;
        this.opener = opener;
        this.random = random;
        this.ids = ids;
        this.clock = clock;
    }

    public Session execute(UUID userId, String areaSlug, Level level) {
        profiles.findByUserId(userId).orElseThrow(CareerProfileNotFoundException::new);
        Area area =
                areas.findBySlug(areaSlug)
                        .filter(Area::active)
                        .orElseThrow(() -> new UnknownAreaException(areaSlug));
        SessionConfig config = area.sessionConfig();
        List<UUID> questions =
                pool.activeQuestions(area.id(), level).stream().map(Question::id).toList();
        List<UUID> challenges =
                pool.activeChallenges(area.id(), level).stream().map(Challenge::id).toList();
        if (questions.size() < config.questionCount()
                || challenges.size() < config.challengeCount()) {
            throw new InsufficientPoolException(area.name(), level);
        }
        return opener.open(
                Session.simulation(
                        ids.next(),
                        userId,
                        area.id(),
                        level,
                        config,
                        SessionAssembly.draw(questions, config.questionCount(), random),
                        SessionAssembly.draw(challenges, config.challengeCount(), random),
                        clock.now()));
    }
}
