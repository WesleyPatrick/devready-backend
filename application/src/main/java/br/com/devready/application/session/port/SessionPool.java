package br.com.devready.application.session.port;

import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.Question;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SessionPool {

    Optional<Area> area(UUID areaId);

    List<Question> activeQuestions(UUID areaId, Level level);

    List<Challenge> activeChallenges(UUID areaId, Level level);

    List<Question> activeQuestionsTagged(UUID areaId, Set<String> tagSlugs, Set<Level> levels);

    List<Question> questions(Collection<UUID> ids);

    List<Challenge> challenges(Collection<UUID> ids);
}
