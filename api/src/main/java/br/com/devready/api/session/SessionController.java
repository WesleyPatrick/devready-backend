package br.com.devready.api.session;

import br.com.devready.api.identity.AuthenticatedUser;
import br.com.devready.api.practice.dto.AnswerRequest;
import br.com.devready.api.practice.dto.CodeRequest;
import br.com.devready.api.practice.dto.SubmissionTicketResponse;
import br.com.devready.api.session.dto.FinishResponse;
import br.com.devready.api.session.dto.SessionAnswerResponse;
import br.com.devready.api.session.dto.SessionResponse;
import br.com.devready.api.session.dto.SessionResultResponse;
import br.com.devready.api.session.dto.SessionSummaryResponse;
import br.com.devready.api.session.dto.StartLessonRequest;
import br.com.devready.api.session.dto.StartSimulationRequest;
import br.com.devready.application.gamification.usecase.AwardAchievementsUseCase;
import br.com.devready.application.gamification.usecase.GetSessionAchievementsUseCase;
import br.com.devready.application.session.dto.SessionAnswer;
import br.com.devready.application.session.usecase.AnswerSessionQuestionUseCase;
import br.com.devready.application.session.usecase.FinishSessionUseCase;
import br.com.devready.application.session.usecase.GetCurrentSessionUseCase;
import br.com.devready.application.session.usecase.GetSessionResultUseCase;
import br.com.devready.application.session.usecase.GetSessionUseCase;
import br.com.devready.application.session.usecase.ListSessionsUseCase;
import br.com.devready.application.session.usecase.RunSessionCodeUseCase;
import br.com.devready.application.session.usecase.StartLessonUseCase;
import br.com.devready.application.session.usecase.StartSimulationUseCase;
import br.com.devready.application.session.usecase.SubmitSessionCodeUseCase;
import br.com.devready.domain.gamification.Achievement;
import br.com.devready.domain.session.Session;
import br.com.devready.domain.session.SessionFormat;
import br.com.devready.domain.session.SessionStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionController {

    private final StartLessonUseCase startLesson;
    private final StartSimulationUseCase startSimulation;
    private final GetSessionUseCase getSession;
    private final GetCurrentSessionUseCase getCurrent;
    private final ListSessionsUseCase listSessions;
    private final AnswerSessionQuestionUseCase answerQuestion;
    private final RunSessionCodeUseCase runCode;
    private final SubmitSessionCodeUseCase submitCode;
    private final FinishSessionUseCase finish;
    private final GetSessionResultUseCase getResult;
    private final AwardAchievementsUseCase award;
    private final GetSessionAchievementsUseCase sessionAchievements;

    public SessionController(
            StartLessonUseCase startLesson,
            StartSimulationUseCase startSimulation,
            GetSessionUseCase getSession,
            GetCurrentSessionUseCase getCurrent,
            ListSessionsUseCase listSessions,
            AnswerSessionQuestionUseCase answerQuestion,
            RunSessionCodeUseCase runCode,
            SubmitSessionCodeUseCase submitCode,
            FinishSessionUseCase finish,
            GetSessionResultUseCase getResult,
            AwardAchievementsUseCase award,
            GetSessionAchievementsUseCase sessionAchievements) {
        this.startLesson = startLesson;
        this.startSimulation = startSimulation;
        this.getSession = getSession;
        this.getCurrent = getCurrent;
        this.listSessions = listSessions;
        this.answerQuestion = answerQuestion;
        this.runCode = runCode;
        this.submitCode = submitCode;
        this.finish = finish;
        this.getResult = getResult;
        this.award = award;
        this.sessionAchievements = sessionAchievements;
    }

    @PostMapping("/lessons")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse lesson(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StartLessonRequest request) {
        UUID user = AuthenticatedUser.id(jwt);
        Session session = startLesson.execute(user, request.competency());
        return SessionResponse.from(getSession.execute(user, session.id()));
    }

    @PostMapping("/simulations")
    @ResponseStatus(HttpStatus.CREATED)
    public SessionResponse simulation(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody StartSimulationRequest request) {
        UUID user = AuthenticatedUser.id(jwt);
        Session session = startSimulation.execute(user, request.area(), request.level());
        return SessionResponse.from(getSession.execute(user, session.id()));
    }

    @GetMapping("/current")
    public SessionResponse current(@AuthenticationPrincipal Jwt jwt) {
        return SessionResponse.from(getCurrent.execute(AuthenticatedUser.id(jwt)));
    }

    @GetMapping
    public List<SessionSummaryResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return listSessions.execute(AuthenticatedUser.id(jwt)).stream()
                .map(SessionSummaryResponse::from)
                .toList();
    }

    @GetMapping("/{id}")
    public SessionResponse session(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        return SessionResponse.from(getSession.execute(AuthenticatedUser.id(jwt), id));
    }

    @PutMapping("/{id}/questions/{position}")
    public SessionAnswerResponse answer(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @PathVariable("position") int position,
            @Valid @RequestBody AnswerRequest request) {
        UUID user = AuthenticatedUser.id(jwt);
        SessionAnswer answer = answerQuestion.execute(user, id, position, request.optionId());
        List<Achievement> fresh =
                answer.question().revealed() || answer.xpGained() > 0
                        ? award.execute(user)
                        : List.of();
        return SessionAnswerResponse.from(answer, fresh);
    }

    @PostMapping("/{id}/challenges/{position}/runs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SubmissionTicketResponse run(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @PathVariable("position") int position,
            @Valid @RequestBody CodeRequest request) {
        return SubmissionTicketResponse.from(
                runCode.execute(
                        AuthenticatedUser.id(jwt),
                        id,
                        position,
                        request.language(),
                        request.sourceCode()));
    }

    @PostMapping("/{id}/challenges/{position}/submissions")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SubmissionTicketResponse submit(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") UUID id,
            @PathVariable("position") int position,
            @Valid @RequestBody CodeRequest request) {
        return SubmissionTicketResponse.from(
                submitCode.execute(
                        AuthenticatedUser.id(jwt),
                        id,
                        position,
                        request.language(),
                        request.sourceCode()));
    }

    @PostMapping("/{id}/finish")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public FinishResponse finish(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        UUID user = AuthenticatedUser.id(jwt);
        Session session = finish.execute(user, id);
        if (session.status() == SessionStatus.COMPLETED
                && session.format() == SessionFormat.SIMULATION) {
            award.execute(user, session.id());
        }
        return new FinishResponse(session.status().name());
    }

    @GetMapping("/{id}/result")
    public SessionResultResponse result(
            @AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
        UUID user = AuthenticatedUser.id(jwt);
        return SessionResultResponse.from(
                getResult.execute(user, id), sessionAchievements.execute(user, id));
    }
}
