package br.com.devready.application.career.port;

import br.com.devready.application.career.dto.CareerSeed;

public interface CareerSeedSource {
    CareerSeed load();
}
