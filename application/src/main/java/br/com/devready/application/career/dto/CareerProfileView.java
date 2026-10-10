package br.com.devready.application.career.dto;

import br.com.devready.domain.career.Company;
import br.com.devready.domain.career.Stack;
import br.com.devready.domain.catalog.Level;
import java.util.List;

public record CareerProfileView(List<Company> companies, Stack stack, Level seniority) {}
