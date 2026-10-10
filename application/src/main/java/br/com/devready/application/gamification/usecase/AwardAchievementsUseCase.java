package br.com.devready.application.gamification.usecase;

import br.com.devready.application.career.dto.TrackCompetencyView;
import br.com.devready.application.career.service.TrackAssembler;
import br.com.devready.application.gamification.port.AchievementRepository;
import br.com.devready.application.practice.port.AnswerRepository;
import br.com.devready.application.practice.port.ChallengeAttemptReader;
import br.com.devready.application.practice.port.SimulationRecordReader;
import br.com.devready.application.shared.Clock;
import br.com.devready.domain.career.CompetencyStatus;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.gamification.AchievementRules;
import br.com.devready.domain.gamification.AchievementSnapshot;
import br.com.devready.domain.gamification.UnlockedAchievement;
import br.com.devready.domain.practice.ActivityDays;
import br.com.devready.domain.practice.Answer;
import br.com.devready.domain.practice.ChallengeAttempt;
import br.com.devready.domain.practice.CompletedSimulation;
import br.com.devready.domain.practice.PracticeProgress;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class AwardAchievementsUseCase {

    private final AnswerRepository answers;
    private final ChallengeAttemptReader attempts;
    private final SimulationRecordReader simulations;
    private final TrackAssembler assembler;
    private final AchievementRepository achievements;
    private final Clock clock;

    public AwardAchievementsUseCase(
            AnswerRepository answers,
            ChallengeAttemptReader attempts,
            SimulationRecordReader simulations,
            TrackAssembler assembler,
            AchievementRepository achievements,
            Clock clock) {
        this.answers = answers;
        this.attempts = attempts;
        this.simulations = simulations;
        this.assembler = assembler;
        this.achievements = achievements;
        this.clock = clock;
    }

    public List<Achievement> execute(UUID userId) {
        return award(userId, null);
    }

    public List<Achievement> execute(UUID userId, UUID sessionId) {
        return award(userId, sessionId);
    }

    private List<Achievement> award(UUID userId, UUID sessionId) {
        Instant now = clock.now();
        List<Answer> mine = answers.findByUser(userId);
        List<ChallengeAttempt> tries = attempts.findByUser(userId);
        List<CompletedSimulation> done = simulations.completedBy(userId);
        PracticeProgress progress = PracticeProgress.of(mine, tries, done, now);
        Set<UUID> solved = new HashSet<>(simulations.questionsSolvedBy(userId));
        mine.stream().filter(Answer::correct).map(Answer::questionId).forEach(solved::add);
        boolean competencyDone =
                assembler
                        .find(userId)
                        .map(
                                track ->
                                        track.competencies().stream()
                                                .map(TrackCompetencyView::status)
                                                .anyMatch(
                                                        status -> status == CompetencyStatus.DONE))
                        .orElse(false);
        AchievementSnapshot snapshot =
                new AchievementSnapshot(
                        mine.size(),
                        solved.size(),
                        progress.streakDays(),
                        progress.xp(),
                        ActivityDays.goalDays(ActivityDays.of(mine, tries, done)),
                        competencyDone,
                        (int) done.stream().filter(CompletedSimulation::counts).count());
        Set<Achievement> unlocked = EnumSet.noneOf(Achievement.class);
        achievements.findByUser(userId).forEach(u -> unlocked.add(u.achievement()));
        List<Achievement> fresh = new ArrayList<>();
        for (Achievement achievement : AchievementRules.newlyEarned(snapshot, unlocked)) {
            if (achievements.unlock(new UnlockedAchievement(userId, achievement, now, sessionId))) {
                fresh.add(achievement);
            }
        }
        return fresh;
    }
}
