package br.com.devready.application.identity.fake;

import br.com.devready.application.identity.port.RefreshTokenStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class InMemoryRefreshTokenStore implements RefreshTokenStore {
    private final Map<String, UUID> active = new HashMap<>();
    private final Map<String, UUID> used = new HashMap<>();
    private int counter = 0;

    @Override
    public String issue(UUID userId) {
        counter++;
        String token = "refresh-" + counter;
        active.put(token, userId);
        return token;
    }

    @Override
    public ConsumeResult consume(String token) {
        UUID userId = active.remove(token);
        if (userId != null) {
            used.put(token, userId);
            return ConsumeResult.valid(userId);
        }
        UUID reusedBy = used.get(token);
        if (reusedBy != null) {
            return ConsumeResult.reused(reusedBy);
        }
        return ConsumeResult.unknown();
    }

    @Override
    public void revoke(String token) {
        active.remove(token);
    }

    @Override
    public void revokeAll(UUID userId) {
        Set<String> tokens = new HashSet<>();
        active.forEach(
                (token, owner) -> {
                    if (owner.equals(userId)) {
                        tokens.add(token);
                    }
                });
        tokens.forEach(active::remove);
    }

    public boolean isActive(String token) {
        return active.containsKey(token);
    }

    public int activeCount(UUID userId) {
        return (int) active.values().stream().filter(userId::equals).count();
    }
}
