package br.com.devready.infrastructure.shared;

import br.com.devready.application.shared.IdGenerator;
import java.util.UUID;

public class RandomIdGenerator implements IdGenerator {
    @Override
    public UUID next() {
        return UUID.randomUUID();
    }
}
