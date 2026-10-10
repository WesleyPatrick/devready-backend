package br.com.devready.domain.evaluation;

public record HarnessLine(int position, Object output, String error, Integer timeMs) {}
