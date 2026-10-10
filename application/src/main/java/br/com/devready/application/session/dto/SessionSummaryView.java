package br.com.devready.application.session.dto;

import br.com.devready.domain.career.Competency;
import br.com.devready.domain.catalog.Area;
import br.com.devready.domain.session.Session;

public record SessionSummaryView(Session session, Area area, Competency competency) {}
