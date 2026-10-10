package br.com.devready.application.catalog.port;

import br.com.devready.application.catalog.dto.CatalogSeed;

public interface CatalogSeedSource {
    CatalogSeed load();
}
