package br.com.devready.application.practice.dto;

import java.util.List;

public record ProblemPage(List<ProblemView> items, int page, int totalPages, int totalItems) {}
