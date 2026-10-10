package br.com.devready.api.practice.dto;

import br.com.devready.application.practice.dto.SubmissionTicket;
import java.util.UUID;

public record SubmissionTicketResponse(UUID id, String status) {
    public static SubmissionTicketResponse from(SubmissionTicket ticket) {
        return new SubmissionTicketResponse(ticket.id(), ticket.status().name());
    }
}
