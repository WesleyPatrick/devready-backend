package br.com.devready.application.identity.port;

import java.util.UUID;

public interface RefreshTokenStore {
    String issue(UUID userId);

    ConsumeResult consume(String token);

    void revoke(String token);

    void revokeAll(UUID userId);

    record ConsumeResult(Status status, UUID userId) {
        public static ConsumeResult unknown() {
            return new ConsumeResult(Status.UNKNOWN, null);
        }

        public static ConsumeResult valid(UUID userId) {
            return new ConsumeResult(Status.VALID, userId);
        }

        public static ConsumeResult reused(UUID userId) {
            return new ConsumeResult(Status.REUSED, userId);
        }
    }

    enum Status {
        VALID,
        REUSED,
        UNKNOWN
    }
}
