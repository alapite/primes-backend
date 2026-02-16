package ereh.won.otbackend.cache;

import java.time.Instant;

public final class PrimeCacheSelection {

    private final PrimeCacheProperties.Backend configuredBackend;
    private final PrimeCacheProperties.Backend effectiveBackend;
    private final FallbackReason fallbackReason;
    private final Instant resolvedAt;

    public PrimeCacheSelection(PrimeCacheProperties.Backend configuredBackend,
                               PrimeCacheProperties.Backend effectiveBackend,
                               FallbackReason fallbackReason) {
        this.configuredBackend = configuredBackend;
        this.effectiveBackend = effectiveBackend;
        this.fallbackReason = fallbackReason;
        this.resolvedAt = Instant.now();
    }

    public PrimeCacheProperties.Backend getConfiguredBackend() {
        return configuredBackend;
    }

    public PrimeCacheProperties.Backend getEffectiveBackend() {
        return effectiveBackend;
    }

    public FallbackReason getFallbackReason() {
        return fallbackReason;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public boolean isFallback() {
        return fallbackReason != null;
    }

    public enum FallbackReason {
        NOT_CONFIGURED,
        INVALID_VALUE,
        CONNECTIVITY_FAILED
    }
}
