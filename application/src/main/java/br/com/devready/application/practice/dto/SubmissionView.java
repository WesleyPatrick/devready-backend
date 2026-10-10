package br.com.devready.application.practice.dto;

import br.com.devready.domain.catalog.Challenge;
import br.com.devready.domain.catalog.TestCase;
import br.com.devready.domain.evaluation.CaseResult;
import br.com.devready.domain.evaluation.CodeSubmission;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public record SubmissionView(
        CodeSubmission submission, String challengeSlug, List<CaseView> cases, boolean sealed) {

    public SubmissionView {
        cases = List.copyOf(cases);
    }

    public static SubmissionView of(CodeSubmission submission, Challenge challenge) {
        Map<Integer, TestCase> byPosition =
                challenge.testCases().stream()
                        .collect(Collectors.toMap(TestCase::position, Function.identity()));
        List<CaseView> cases =
                submission.results().stream()
                        .sorted(Comparator.comparingInt(CaseResult::position))
                        .map(result -> CaseView.of(result, byPosition.get(result.position())))
                        .toList();
        return new SubmissionView(submission, challenge.slug(), cases, false);
    }

    public SubmissionView sealedView() {
        CodeSubmission s = submission;
        CodeSubmission bare =
                new CodeSubmission(
                        s.id(),
                        s.userId(),
                        s.challengeId(),
                        s.language(),
                        s.sourceCode(),
                        s.kind(),
                        s.status(),
                        s.passedCount(),
                        s.totalCount(),
                        null,
                        null,
                        s.xpReward(),
                        s.xpGained(),
                        s.createdAt(),
                        s.finishedAt(),
                        List.of(),
                        s.sessionId());
        return new SubmissionView(bare, challengeSlug, List.of(), true);
    }
}
