package br.com.devready.application.practice.usecase;

import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.application.practice.port.ChallengeAttemptReader;
import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.application.shared.Clock;
import br.com.devready.domain.practice.PracticeProgress;
import java.util.UUID;

public class GetProgressUseCase {

    private final AnswerRepository answers;
    private final ChallengeAttemptReader attempts;
    private final SimulationRecordReader simulations;
    private final Clock clock;

    public GetProgressUseCase(
            AnswerRepository answers,
            ChallengeAttemptReader attempts,
            SimulationRecordReader simulations,
            Clock clock) {
        this.answers = answers;
        this.attempts = attempts;
        this.simulations = simulations;
        this.clock = clock;
    }

    public PracticeProgress execute(UUID userId) {
        return PracticeProgress.of(
                answers.findByUser(userId),
                attempts.findByUser(userId),
                simulations.completedBy(userId),
                clock.now());
    }
}
