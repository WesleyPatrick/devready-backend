package br.com.devready.api.practice;

import br.com.devready.api.identity.AuthenticatedUser;
import br.com.devready.api.practice.dto.AnswerRequest;
import br.com.devready.api.practice.dto.AnswerResultResponse;
import br.com.devready.api.practice.dto.ProgressResponse;
import br.com.devready.api.practice.dto.QuestionPageResponse;
import br.com.devready.api.practice.dto.QuestionResponse;
import br.com.devready.application.gamification.usecase.AwardAchievementsUseCase;
import br.com.devready.application.practice.dto.AnswerResult;
import br.com.devready.application.practice.dto.ProblemQuery;
import br.com.devready.application.practice.usecase.AnswerQuestionUseCase;
import br.com.devready.application.practice.usecase.GetDailyQuestionUseCase;
import br.com.devready.application.practice.usecase.GetProgressUseCase;
import br.com.devready.application.practice.usecase.GetQuestionUseCase;
import br.com.devready.application.practice.usecase.ListProblemsUseCase;
import br.com.devready.domain.catalog.Difficulty;
import br.com.devready.domain.catalog.Level;
import br.com.devready.domain.catalog.ProblemType;
import br.com.devready.domain.practice.QuestionStatus;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PracticeController {

    private final ListProblemsUseCase listProblems;
    private final GetQuestionUseCase getQuestion;
    private final GetDailyQuestionUseCase getDailyQuestion;
    private final AnswerQuestionUseCase answerQuestion;
    private final GetProgressUseCase getProgress;
    private final AwardAchievementsUseCase award;

    public PracticeController(
            ListProblemsUseCase listProblems,
            GetQuestionUseCase getQuestion,
            GetDailyQuestionUseCase getDailyQuestion,
            AnswerQuestionUseCase answerQuestion,
            GetProgressUseCase getProgress,
            AwardAchievementsUseCase award) {
        this.listProblems = listProblems;
        this.getQuestion = getQuestion;
        this.getDailyQuestion = getDailyQuestion;
        this.answerQuestion = answerQuestion;
        this.getProgress = getProgress;
        this.award = award;
    }

    @GetMapping("/questions")
    public QuestionPageResponse list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "type", required = false) ProblemType type,
            @RequestParam(name = "status", required = false) QuestionStatus status,
            @RequestParam(name = "difficulty", required = false) Difficulty difficulty,
            @RequestParam(name = "competency", required = false) String competency,
            @RequestParam(name = "company", required = false) String company,
            @RequestParam(name = "level", required = false) Level level) {
        return QuestionPageResponse.from(
                listProblems.execute(
                        new ProblemQuery(
                                AuthenticatedUser.id(jwt),
                                page,
                                size,
                                type,
                                status,
                                difficulty,
                                competency,
                                company,
                                level)));
    }

    @GetMapping("/questions/daily")
    public QuestionResponse daily(@AuthenticationPrincipal Jwt jwt) {
        return QuestionResponse.from(getDailyQuestion.execute(AuthenticatedUser.id(jwt)));
    }

    @GetMapping("/questions/{slug}")
    public QuestionResponse question(
            @AuthenticationPrincipal Jwt jwt, @PathVariable("slug") String slug) {
        return QuestionResponse.from(getQuestion.execute(AuthenticatedUser.id(jwt), slug));
    }

    @PostMapping("/questions/{slug}/answer")
    public AnswerResultResponse answer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("slug") String slug,
            @Valid @RequestBody AnswerRequest request) {
        UUID user = AuthenticatedUser.id(jwt);
        AnswerResult result = answerQuestion.execute(user, slug, request.optionId());
        return AnswerResultResponse.from(result, award.execute(user));
    }

    @GetMapping("/me/progress")
    public ProgressResponse progress(@AuthenticationPrincipal Jwt jwt) {
        return ProgressResponse.from(getProgress.execute(AuthenticatedUser.id(jwt)));
    }
}
