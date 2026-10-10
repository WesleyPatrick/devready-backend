package br.com.devready.api.career;

import br.com.devready.api.career.dto.CareerProfileResponse;
import br.com.devready.api.career.dto.CompetencyDetailResponse;
import br.com.devready.api.career.dto.OnboardingOptionsResponse;
import br.com.devready.api.career.dto.SaveCareerProfileRequest;
import br.com.devready.api.career.dto.TrackResponse;
import br.com.devready.api.identity.AuthenticatedUser;
import br.com.devready.application.career.dto.SaveCareerProfileCommand;
import br.com.devready.application.career.usecase.GetCareerProfileUseCase;
import br.com.devready.application.career.usecase.GetLearningTrackUseCase;
import br.com.devready.application.career.usecase.GetTrackCompetencyUseCase;
import br.com.devready.application.career.usecase.ListOnboardingOptionsUseCase;
import br.com.devready.application.career.usecase.SaveCareerProfileUseCase;
import br.com.devready.application.gamification.usecase.AwardAchievementsUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/career")
public class CareerController {

    private final ListOnboardingOptionsUseCase listOptions;
    private final GetCareerProfileUseCase getProfile;
    private final SaveCareerProfileUseCase saveProfile;
    private final GetLearningTrackUseCase getTrack;
    private final GetTrackCompetencyUseCase getCompetency;
    private final AwardAchievementsUseCase award;

    public CareerController(
            ListOnboardingOptionsUseCase listOptions,
            GetCareerProfileUseCase getProfile,
            SaveCareerProfileUseCase saveProfile,
            GetLearningTrackUseCase getTrack,
            GetTrackCompetencyUseCase getCompetency,
            AwardAchievementsUseCase award) {
        this.listOptions = listOptions;
        this.getProfile = getProfile;
        this.saveProfile = saveProfile;
        this.getTrack = getTrack;
        this.getCompetency = getCompetency;
        this.award = award;
    }

    @GetMapping("/options")
    @SecurityRequirements
    public OnboardingOptionsResponse options() {
        return OnboardingOptionsResponse.from(listOptions.execute());
    }

    @GetMapping("/profile")
    public CareerProfileResponse profile(@AuthenticationPrincipal Jwt jwt) {
        return CareerProfileResponse.from(getProfile.execute(AuthenticatedUser.id(jwt)));
    }

    @PutMapping("/profile")
    public CareerProfileResponse save(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody SaveCareerProfileRequest request) {
        return CareerProfileResponse.from(
                saveProfile.execute(
                        new SaveCareerProfileCommand(
                                AuthenticatedUser.id(jwt),
                                request.companies(),
                                request.stack(),
                                request.seniority())));
    }

    @GetMapping("/track")
    public TrackResponse track(@AuthenticationPrincipal Jwt jwt) {
        UUID user = AuthenticatedUser.id(jwt);
        TrackResponse response = TrackResponse.from(getTrack.execute(user));
        award.execute(user);
        return response;
    }

    @GetMapping("/track/competencies/{slug}")
    public CompetencyDetailResponse competency(
            @AuthenticationPrincipal Jwt jwt, @PathVariable("slug") String slug) {
        return CompetencyDetailResponse.from(
                getCompetency.execute(AuthenticatedUser.id(jwt), slug));
    }
}
