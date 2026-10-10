package br.com.devready.api.config;

import br.com.devready.application.career.exception.CareerProfileNotFoundException;
import br.com.devready.application.career.exception.CompetencyNotInTrackException;
import br.com.devready.application.career.exception.UnknownCompanyException;
import br.com.devready.application.career.exception.UnknownStackException;
import br.com.devready.application.identity.exception.CannotChangeOwnAccountException;
import br.com.devready.application.identity.exception.EmailAlreadyRegisteredException;
import br.com.devready.application.identity.exception.InvalidCredentialsException;
import br.com.devready.application.identity.exception.InvalidCurrentPasswordException;
import br.com.devready.application.identity.exception.InvalidOneTimeCodeException;
import br.com.devready.application.identity.exception.InvalidRefreshTokenException;
import br.com.devready.application.identity.exception.LastActiveAdminException;
import br.com.devready.application.identity.exception.NewPasswordSameAsTemporaryException;
import br.com.devready.application.identity.exception.NotAnActiveAdminException;
import br.com.devready.application.identity.exception.PasswordChangedMeanwhileException;
import br.com.devready.application.identity.exception.UserInactiveException;
import br.com.devready.application.identity.exception.UserNotFoundException;
import br.com.devready.application.practice.exception.ChallengeNotFoundException;
import br.com.devready.application.practice.exception.LanguageNotOfferedException;
import br.com.devready.application.practice.exception.NoDailyQuestionException;
import br.com.devready.application.practice.exception.OptionNotInQuestionException;
import br.com.devready.application.practice.exception.ProblemInExamException;
import br.com.devready.application.practice.exception.QuestionNotFoundException;
import br.com.devready.application.practice.exception.SubmissionNotFoundException;
import br.com.devready.application.session.exception.InsufficientPoolException;
import br.com.devready.application.session.exception.ItemNotInSessionException;
import br.com.devready.application.session.exception.LessonUnavailableException;
import br.com.devready.application.session.exception.NoSubmissionsLeftException;
import br.com.devready.application.session.exception.OpenSessionConflictException;
import br.com.devready.application.session.exception.QuestionAlreadyAnsweredException;
import br.com.devready.application.session.exception.SessionClosedException;
import br.com.devready.application.session.exception.SessionInProgressException;
import br.com.devready.application.session.exception.SessionNotFoundException;
import br.com.devready.application.session.exception.SessionResultNotReadyException;
import br.com.devready.application.session.exception.UnknownAreaException;
import br.com.devready.application.shared.exception.ApplicationException;
import br.com.devready.domain.career.CareerException;
import br.com.devready.domain.identity.IdentityException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors =
                ex.getBindingResult().getFieldErrors().stream().map(this::fieldError).toList();
        ProblemDetail problem =
                problem(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "Dados inválidos",
                        "Confira os campos informados.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(InvalidFormatException.class)
    ProblemDetail invalidFormat(InvalidFormatException ex) {
        Class<?> target = ex.getTargetType();
        String message =
                target != null && target.isEnum()
                        ? "Valor inválido. Use um destes: "
                                + Arrays.stream(target.getEnumConstants())
                                        .map(constant -> ((Enum<?>) constant).name())
                                        .collect(Collectors.joining(", "))
                                + "."
                        : "Valor em formato inválido.";
        ProblemDetail problem =
                problem(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "Dados inválidos",
                        "Confira os campos informados.");
        problem.setProperty(
                "errors", List.of(Map.of("field", fieldPath(ex.getPath()), "message", message)));
        return problem;
    }

    private static String fieldPath(List<JacksonException.Reference> path) {
        StringBuilder field = new StringBuilder();
        for (JacksonException.Reference reference : path) {
            if (reference.getPropertyName() != null) {
                if (!field.isEmpty()) {
                    field.append('.');
                }
                field.append(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                field.append('[').append(reference.getIndex()).append(']');
            }
        }
        return field.toString();
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    ProblemDetail conflict(EmailAlreadyRegisteredException ex) {
        return problem(HttpStatus.CONFLICT, "E-mail já cadastrado", ex.getMessage());
    }

    @ExceptionHandler(InvalidCurrentPasswordException.class)
    ProblemDetail currentPassword(InvalidCurrentPasswordException ex) {
        ProblemDetail problem =
                problem(
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        "Dados inválidos",
                        "Confira os campos informados.");
        problem.setProperty(
                "errors",
                List.of(
                        Map.of(
                                "field",
                                InvalidCurrentPasswordException.FIELD,
                                "message",
                                ex.getMessage())));
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integrityViolation(DataIntegrityViolationException ex) {
        return problem(HttpStatus.CONFLICT, "Conflito", "Conflito ao gravar. Tente de novo.");
    }

    @ExceptionHandler({
        InvalidCredentialsException.class,
        InvalidRefreshTokenException.class,
        InvalidOneTimeCodeException.class
    })
    ProblemDetail unauthorized(ApplicationException ex) {
        return problem(HttpStatus.UNAUTHORIZED, "Não autenticado", ex.getMessage());
    }

    @ExceptionHandler(PasswordChangedMeanwhileException.class)
    ProblemDetail passwordChangedMeanwhile(PasswordChangedMeanwhileException ex) {
        return problem(HttpStatus.CONFLICT, "Senha alterada", ex.getMessage());
    }

    @ExceptionHandler(UserInactiveException.class)
    ProblemDetail inactive(UserInactiveException ex) {
        return problem(HttpStatus.FORBIDDEN, "Conta desativada", ex.getMessage());
    }

    @ExceptionHandler(NotAnActiveAdminException.class)
    ProblemDetail notAnActiveAdmin(NotAnActiveAdminException ex) {
        return problem(HttpStatus.FORBIDDEN, "Sem permissão", ex.getMessage());
    }

    @ExceptionHandler({
        UserNotFoundException.class,
        CareerProfileNotFoundException.class,
        CompetencyNotInTrackException.class,
        QuestionNotFoundException.class,
        ChallengeNotFoundException.class,
        SubmissionNotFoundException.class,
        NoDailyQuestionException.class,
        SessionNotFoundException.class,
        SessionResultNotReadyException.class
    })
    ProblemDetail notFound(ApplicationException ex) {
        return problem(HttpStatus.NOT_FOUND, "Não encontrado", ex.getMessage());
    }

    @ExceptionHandler({
        IdentityException.class,
        CannotChangeOwnAccountException.class,
        LastActiveAdminException.class,
        NewPasswordSameAsTemporaryException.class,
        CareerException.class,
        UnknownCompanyException.class,
        UnknownStackException.class,
        OptionNotInQuestionException.class,
        LanguageNotOfferedException.class,
        UnknownAreaException.class,
        InsufficientPoolException.class,
        LessonUnavailableException.class,
        SessionClosedException.class,
        NoSubmissionsLeftException.class,
        ItemNotInSessionException.class,
        QuestionAlreadyAnsweredException.class
    })
    ProblemDetail domainRule(RuntimeException ex) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "Dados inválidos", ex.getMessage());
    }

    @ExceptionHandler(SessionInProgressException.class)
    ProblemDetail sessionInProgress(SessionInProgressException ex) {
        ProblemDetail problem =
                problem(HttpStatus.CONFLICT, "Sessão em andamento", ex.getMessage());
        problem.setProperty("sessionId", ex.sessionId());
        return problem;
    }

    @ExceptionHandler(OpenSessionConflictException.class)
    ProblemDetail sessionConflict(OpenSessionConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Sessão em andamento", ex.getMessage());
    }

    @ExceptionHandler(ProblemInExamException.class)
    ProblemDetail problemInExam(ProblemInExamException ex) {
        ProblemDetail problem =
                problem(HttpStatus.CONFLICT, "Sessão em andamento", ex.getMessage());
        problem.setProperty("sessionId", ex.sessionId());
        return problem;
    }

    @ExceptionHandler(ApplicationException.class)
    ProblemDetail application(ApplicationException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
    }

    private Map<String, String> fieldError(FieldError error) {
        return Map.of(
                "field", error.getField(), "message", String.valueOf(error.getDefaultMessage()));
    }

    private static ProblemDetail problem(HttpStatus status, String title, String detail) {
        return Problems.of(status, title, detail);
    }
}
