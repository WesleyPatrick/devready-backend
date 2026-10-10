package br.com.devready.infrastructure.shared;

import br.com.devready.domain.session.RandomSource;
import java.util.concurrent.ThreadLocalRandom;

public class SystemRandomSource implements RandomSource {
    @Override
    public int nextInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }
}
