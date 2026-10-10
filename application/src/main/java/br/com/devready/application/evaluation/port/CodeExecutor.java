package br.com.devready.application.evaluation.port;

import br.com.devready.application.evaluation.dto.ExecutionRequest;
import br.com.devready.domain.evaluation.ExecutionReport;

public interface CodeExecutor {
    ExecutionReport execute(ExecutionRequest request);
}
