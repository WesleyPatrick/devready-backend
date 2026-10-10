package br.com.devready.application.gamification.dto;

import br.com.devready.domain.gamification.Achievement;
import java.time.Instant;

public record AchievementView(Achievement achievement, Instant unlockedAt) {}
