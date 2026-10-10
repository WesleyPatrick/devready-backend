package br.com.devready.infrastructure.config;

import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.evaluation.port.SubmissionRepository;
import br.com.devready.application.gamification.port.AchievementRepository;
import br.com.devready.application.gamification.usecase.AfterSubmissionUseCase;
import br.com.devready.application.gamification.usecase.AwardAchievementsUseCase;
import br.com.devready.application.gamification.usecase.GetSessionAchievementsUseCase;
import br.com.devready.application.gamification.usecase.ListAchievementsUseCase;
import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.application.practice.port.ChallengeAttemptReader;
import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.application.session.usecase.FinalizeSessionUseCase;
import br.com.devready.application.shared.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GamificationConfiguration {

    @Bean
    AwardAchievementsUseCase awardAchievements(
            AnswerRepository answers,
            ChallengeAttemptReader attempts,
            SimulationRecordReader simulations,
            TrackAssembler assembler,
            AchievementRepository achievements,
            Clock clock) {
        return new AwardAchievementsUseCase(
                answers, attempts, simulations, assembler, achievements, clock);
    }

    @Bean
    ListAchievementsUseCase listAchievements(AchievementRepository achievements) {
        return new ListAchievementsUseCase(achievements);
    }

    @Bean
    GetSessionAchievementsUseCase getSessionAchievements(AchievementRepository achievements) {
        return new GetSessionAchievementsUseCase(achievements);
    }

    @Bean
    AfterSubmissionUseCase afterSubmission(
            SubmissionRepository submissions,
            FinalizeSessionUseCase finalizeSession,
            AwardAchievementsUseCase awardAchievements) {
        return new AfterSubmissionUseCase(submissions, finalizeSession, awardAchievements);
    }
}
