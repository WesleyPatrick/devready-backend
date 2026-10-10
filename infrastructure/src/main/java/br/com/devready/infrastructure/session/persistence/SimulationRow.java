package br.com.devready.infrastructure.session.persistence;

import java.time.Instant;

public record SimulationRow(Instant finishedAt, Integer xpAwarded) {}
