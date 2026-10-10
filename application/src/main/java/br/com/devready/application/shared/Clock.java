package br.com.devready.application.shared;

import java.time.Instant;

public interface Clock {
    Instant now();
}
