package br.com.devready.api.practice;

import br.com.devready.api.identity.AuthenticatedUser;
import br.com.devready.api.practice.dto.ChallengeResponse;
import br.com.devready.api.practice.dto.CodeRequest;
import br.com.devready.api.practice.dto.SubmissionResponse;
import br.com.devready.api.practice.dto.SubmissionSummaryResponse;
import br.com.devready.api.practice.dto.SubmissionTicketResponse;
import br.com.devready.application.practice.usecase.GetChallengeUseCase;
import br.com.devready.application.practice.usecase.ListChallengeSubmissionsUseCase;
import br.com.devready.application.practice.usecase.RunCodeUseCase;
import br.com.devready.application.practice.usecase.SubmitCodeUseCase;
import br.com.devready.application.session.usecase.ViewSubmissionUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ChallengeController {

    private final GetChallengeUseCase getChallenge;
    private final RunCodeUseCase runCode;
    private final SubmitCodeUseCase submitCode;
    private final ViewSubmissionUseCase viewSubmission;
    private final ListChallengeSubmissionsUseCase listSubmissions;

    public ChallengeController(
            GetChallengeUseCase getChallenge,
            RunCodeUseCase runCode,
            SubmitCodeUseCase submitCode,
            ViewSubmissionUseCase viewSubmission,
            ListChallengeSubmissionsUseCase listSubmissions) {
        this.getChallenge = getChallenge;
        this.runCode = runCode;
        this.submitCode = submitCode;
        this.viewSubmission = viewSubmission;
        this.listSubmissions = listSubmissions;
    }

    @GetMapping("/challenges/{slug}")
    public ChallengeResponse challenge(
            @AuthenticationPrincipal Jwt jwt, @PathVariable("slug") String slug) {
        return ChallengeResponse.from(getChallenge.execute(AuthenticatedUser.id(jwt), slug));
    }

    @PostMapping("/challenges/{slug}/runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SubmissionTicketResponse run(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("slug") String slug,
            @Valid @RequestBody CodeRequest request) {
        return SubmissionTicketResponse.from(
                runCode.execute(
                        AuthenticatedUser.id(jwt), slug, request.language(), request.sourceCode()));
    }

    @PostMapping("/challenges/{slug}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SubmissionTicketResponse submit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("slug") String slug,
            @Valid @RequestBody CodeRequest request) {
        return SubmissionTicketResponse.from(
                submitCode.execute(
                        AuthenticatedUser.id(jwt), slug, request.language(), request.sourceCode()));
    }

    @GetMapping("/challenges/{slug}/submissions")
    public List<SubmissionSummaryResponse> submissions(
            @AuthenticationPrincipal Jwt jwt, @PathVariable("slug") String slug) {
        return listSubmissions.execute(AuthenticatedUser.id(jwt), slug).stream()
                .map(SubmissionSummaryResponse::from)
                .toList();
    }

    @GetMapping("/submissions/{id}")
    public SubmissionResponse submission(
            @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return SubmissionResponse.from(viewSubmission.execute(AuthenticatedUser.id(jwt), id));
    }
}
