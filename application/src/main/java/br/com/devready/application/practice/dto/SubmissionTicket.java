package br.com.devready.application.practice.dto;

import br.com.devready.domain.evaluation.SubmissionStatus;
import java.util.UUID;

public record SubmissionTicket(UUID id, SubmissionStatus status) {}
