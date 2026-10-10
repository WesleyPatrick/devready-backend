package br.com.devready.application.shared;

import java.time.Instant;

public class FixedClock implements Clock {
    private Instant now;

    public FixedClock(Instant now) {
        this.now = now;
    }

    @Override
    public Instant now() {
        return now;
    }

    public void advanceTo(Instant instant) {
        this.now = instant;
    }
}
