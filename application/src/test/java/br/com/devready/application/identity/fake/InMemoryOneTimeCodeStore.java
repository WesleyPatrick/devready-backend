package br.com.devready.application.identity.fake;

import br.com.devready.application.identity.port.OneTimeCodeStore;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryOneTimeCodeStore implements OneTimeCodeStore {
    private final Map<String, UUID> codes = new HashMap<>();
    private int counter = 0;

    @Override
    public String issue(UUID userId) {
        counter++;
        String code = "code-" + counter;
        codes.put(code, userId);
        return code;
    }

    @Override
    public Optional<UUID> consume(String code) {
        return Optional.ofNullable(codes.remove(code));
    }
}
