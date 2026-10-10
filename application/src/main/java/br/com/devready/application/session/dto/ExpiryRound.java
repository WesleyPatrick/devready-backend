package br.com.devready.application.session.dto;

import br.com.devready.domain.session.Session;
import java.util.List;

public record ExpiryRound(int expired, List<Session> closed) {}
