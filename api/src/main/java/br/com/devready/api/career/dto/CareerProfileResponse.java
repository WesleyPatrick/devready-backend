package br.com.devready.api.career.dto;

import br.com.devready.application.career.dto.CareerProfileView;
import java.util.List;

public record CareerProfileResponse(
        List<CompanyOptionResponse> companies, StackOptionResponse stack, String seniority) {
    public static CareerProfileResponse from(CareerProfileView view) {
        return new CareerProfileResponse(
                view.companies().stream().map(CompanyOptionResponse::from).toList(),
                StackOptionResponse.from(view.stack()),
                view.seniority().name());
    }
}
