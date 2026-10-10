package br.com.devready.infrastructure.shared;

import br.com.devready.application.shared.Sleeper;
import java.time.Duration;

public class ThreadSleeper implements Sleeper {
    @Override
    public void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Espera interrompida.", e);
        }
    }
}
