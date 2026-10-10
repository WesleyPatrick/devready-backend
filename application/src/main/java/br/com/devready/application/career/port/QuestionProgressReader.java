package br.com.devready.application.career.port;

import br.com.devready.domain.career.QuestionProgress;
import java.util.Map;
import java.util.UUID;

public interface QuestionProgressReader {
    Map<String, QuestionProgress> answeredBy(UUID userId);
}
