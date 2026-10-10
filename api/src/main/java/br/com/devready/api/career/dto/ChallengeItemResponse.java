package br.com.devready.api.career.dto;

import br.com.devready.application.career.dto.TrackChallengeView;
import br.com.devready.domain.catalog.Language;
import java.util.List;

public record ChallengeItemResponse(
        String slug,
        String title,
        String difficulty,
        String level,
        List<String> languages,
        String status) {
    public static ChallengeItemResponse from(TrackChallengeView challenge) {
        return new ChallengeItemResponse(
                challenge.slug(),
                challenge.title(),
                challenge.difficulty().name(),
                challenge.level().name(),
                challenge.languages().stream().map(Language::name).toList(),
                challenge.progress().name());
    }
}
