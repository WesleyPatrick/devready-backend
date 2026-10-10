package br.com.devready.application.session.dto;

import br.com.devready.domain.session.SessionStatus;

public record SessionAnswer(
        SessionQuestionView question, int xpGained, SessionStatus sessionStatus) {}
