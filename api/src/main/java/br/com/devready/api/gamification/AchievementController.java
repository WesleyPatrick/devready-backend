package br.com.devready.api.gamification;

import br.com.devready.api.gamification.dto.AchievementResponse;
import br.com.devready.api.identity.AuthenticatedUser;
import br.com.devready.application.gamification.usecase.ListAchievementsUseCase;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
public class AchievementController {

    private final ListAchievementsUseCase listAchievements;

    public AchievementController(ListAchievementsUseCase listAchievements) {
        this.listAchievements = listAchievements;
    }

    @GetMapping("/achievements")
    public List<AchievementResponse> achievements(@AuthenticationPrincipal Jwt jwt) {
        return listAchievements.execute(AuthenticatedUser.id(jwt)).stream()
                .map(AchievementResponse::from)
                .toList();
    }
}
