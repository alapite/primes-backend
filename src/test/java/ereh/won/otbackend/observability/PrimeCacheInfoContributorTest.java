package ereh.won.otbackend.observability;

import ereh.won.otbackend.cache.PrimeCacheProperties;
import ereh.won.otbackend.cache.PrimeCacheSelection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.info.Info.Builder;

import java.util.Map;

import static org.mockito.Mockito.*;

class PrimeCacheInfoContributorTest {

    private PrimeCacheSelection cacheSelection;
    private PrimeCacheInfoContributor contributor;
    private Builder infoBuilder;

    @BeforeEach
    void setUp() {
        cacheSelection = mock(PrimeCacheSelection.class);
        contributor = new PrimeCacheInfoContributor(cacheSelection);
        infoBuilder = mock(Builder.class);
    }

    @Test
    void contribute_includesConfiguredAndEffectiveBackend() {
        when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.REDIS);
        when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.REDIS);
        when(cacheSelection.isFallback()).thenReturn(false);

        contributor.contribute(infoBuilder);

        verify(infoBuilder).withDetail(eq("primeCache"), any(Map.class));
    }

    @Test
    void contribute_includesFallbackReasonWhenFallbackOccurred() {
        when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.REDIS);
        when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
        when(cacheSelection.isFallback()).thenReturn(true);
        when(cacheSelection.getFallbackReason()).thenReturn(PrimeCacheSelection.FallbackReason.CONNECTIVITY_FAILED);

        contributor.contribute(infoBuilder);

        verify(infoBuilder).withDetail(eq("primeCache"), argThat((Map<?, ?> map) ->
            "CONNECTIVITY_FAILED".equals(map.get("fallbackReason"))
        ));
    }

    @Test
    void contribute_noFallbackReasonWhenNoFallback() {
        when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
        when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
        when(cacheSelection.isFallback()).thenReturn(false);

        contributor.contribute(infoBuilder);

        verify(infoBuilder).withDetail(eq("primeCache"), argThat((Map<?, ?> map) ->
            !map.containsKey("fallbackReason")
        ));
    }

    @Test
    void contribute_mapsConfiguredBackendToString() {
        when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.POSTGRES);
        when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.POSTGRES);
        when(cacheSelection.isFallback()).thenReturn(false);

        contributor.contribute(infoBuilder);

        verify(infoBuilder).withDetail(eq("primeCache"), argThat((Map<?, ?> map) ->
            "POSTGRES".equals(map.get("configuredBackend"))
        ));
    }

    @Test
    void contribute_mapsEffectiveBackendToString() {
        when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.REDIS);
        when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
        when(cacheSelection.isFallback()).thenReturn(true);
        when(cacheSelection.getFallbackReason()).thenReturn(PrimeCacheSelection.FallbackReason.INVALID_VALUE);

        contributor.contribute(infoBuilder);

        verify(infoBuilder).withDetail(eq("primeCache"), argThat((Map<?, ?> map) ->
            "MEMORY".equals(map.get("effectiveBackend"))
        ));
    }
}
