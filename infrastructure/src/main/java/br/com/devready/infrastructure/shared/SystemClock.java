package br.com.devready.infrastructure.shared;

import br.com.devready.application.shared.Clock;
import java.time.Instant;

public class SystemClock implements Clock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
