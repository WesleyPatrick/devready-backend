package br.com.devready.application.evaluation.fake;

import br.com.devready.application.shared.Sleeper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class RecordingSleeper implements Sleeper {
    private final List<Duration> slept = new ArrayList<>();

    @Override
    public void sleep(Duration duration) {
        slept.add(duration);
    }

    public List<Duration> slept() {
        return List.copyOf(slept);
    }
}
