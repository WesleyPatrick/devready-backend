package br.com.devready.application.session.dto;

import java.util.UUID;

public record ChallengeCorrectionView(
        int position,
        String slug,
        String title,
        int passedCount,
        int totalCount,
        UUID bestSubmissionId) {}
