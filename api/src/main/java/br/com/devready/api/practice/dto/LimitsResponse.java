package br.com.devready.api.practice.dto;

import br.com.devready.domain.catalog.ExecutionLimits;

public record LimitsResponse(int timeLimitMs, int memoryLimitKb) {
    public static LimitsResponse from(ExecutionLimits limits) {
        return new LimitsResponse(limits.timeLimitMs(), limits.memoryLimitKb());
    }
}
