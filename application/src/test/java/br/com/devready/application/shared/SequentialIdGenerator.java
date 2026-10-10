package br.com.devready.application.shared;

import java.util.UUID;

public class SequentialIdGenerator implements IdGenerator {
    private long counter = 0;

    @Override
    public UUID next() {
        counter++;
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", counter));
    }
}
