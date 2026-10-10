package br.com.devready.api.catalog;

import br.com.devready.api.catalog.dto.AreaResponse;
import br.com.devready.application.catalog.usecase.ListAreasUseCase;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/areas")
public class AreaController {

    private final ListAreasUseCase listAreas;

    public AreaController(ListAreasUseCase listAreas) {
        this.listAreas = listAreas;
    }

    @GetMapping
    public List<AreaResponse> list() {
        return listAreas.execute().stream().map(AreaResponse::from).toList();
    }
}
