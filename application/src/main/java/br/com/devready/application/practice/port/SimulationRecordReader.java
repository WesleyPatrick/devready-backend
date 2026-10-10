package br.com.devready.application.practice.port;

import br.com.devready.domain.practice.CompletedSimulation;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface SimulationRecordReader {

    List<CompletedSimulation> completedBy(UUID userId);

    Set<UUID> questionsSolvedBy(UUID userId);

    Set<UUID> challengesSolvedBy(UUID userId);
}
