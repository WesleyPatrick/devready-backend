package br.com.devready.application.session.dto;

import br.com.devready.domain.career.Competency;
import br.com.devready.domain.session.CompetencyFeedback;

public record CompetencyResultView(Competency competency, CompetencyFeedback feedback) {}
