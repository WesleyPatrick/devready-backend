package br.com.devready.application.identity.port;

import java.util.Optional;
import java.util.UUID;

public interface OneTimeCodeStore {
    String issue(UUID userId);

    Optional<UUID> consume(String code);
}
