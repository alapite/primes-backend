package ereh.won.otbackend.observability;

import ereh.won.otbackend.cache.PrimeCacheProperties;
import ereh.won.otbackend.cache.PrimeCacheSelection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PrimeCacheHealthIndicatorTest {

    private PrimeCacheSelection cacheSelection;
    private PrimeCacheHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        cacheSelection = mock(PrimeCacheSelection.class);
        indicator = new PrimeCacheHealthIndicator(cacheSelection);
    }

    @Test
    void health_returnsUpStatus() {
        when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
        when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
        when(cacheSelection.isFallback()).thenReturn(false);

        Health health = indicator.health();

        assertNotNull(health.getStatus());
        assertEquals("UP", health.getStatus().getCode());
    }

    @Test
    void health_includesConfiguredAndEffectiveBackend() {
        when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.REDIS);
        when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.REDIS);
        when(cacheSelection.isFallback()).thenReturn(false);

        Health health = indicator.health();

        assertEquals("REDIS", health.getDetails().get("configuredBackend"));
        assertEquals("REDIS", health.getDetails().get("effectiveBackend"));
    }

	@Test
	void health_includesFallbackReasonWhenFallbackOccurred() {
		when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.REDIS);
		when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
		when(cacheSelection.isFallback()).thenReturn(true);
        when(cacheSelection.getFallbackReason()).thenReturn(PrimeCacheSelection.FallbackReason.CONNECTIVITY_FAILED);

        Health health = indicator.health();

		assertEquals("CONNECTIVITY_FAILED", health.getDetails().get("fallbackReason"));
	}

	@Test
	void health_returnsDegradedStatusWhenFallbackOccurred() {
		when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.POSTGRES);
		when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
		when(cacheSelection.isFallback()).thenReturn(true);
		when(cacheSelection.getFallbackReason()).thenReturn(PrimeCacheSelection.FallbackReason.CONNECTIVITY_FAILED);

		Health health = indicator.health();

		assertEquals("DEGRADED", health.getStatus().getCode());
	}

    @Test
    void health_noFallbackReasonWhenNoFallback() {
        when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
        when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
        when(cacheSelection.isFallback()).thenReturn(false);

        Health health = indicator.health();

        assertFalse(health.getDetails().containsKey("fallbackReason"));
    }

    @Test
    void health_mapsAllBackendTypes() {
        when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.POSTGRES);
        when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.POSTGRES);
        when(cacheSelection.isFallback()).thenReturn(false);

        Health health = indicator.health();

        assertEquals("POSTGRES", health.getDetails().get("configuredBackend"));
        assertEquals("POSTGRES", health.getDetails().get("effectiveBackend"));
    }

    @Test
    void health_reflectsDifferentConfiguredAndEffectiveBackends() {
        when(cacheSelection.getConfiguredBackend()).thenReturn(PrimeCacheProperties.Backend.REDIS);
        when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);
        when(cacheSelection.isFallback()).thenReturn(true);
        when(cacheSelection.getFallbackReason()).thenReturn(PrimeCacheSelection.FallbackReason.INVALID_VALUE);

        Health health = indicator.health();

        assertEquals("REDIS", health.getDetails().get("configuredBackend"));
        assertEquals("MEMORY", health.getDetails().get("effectiveBackend"));
        assertEquals("INVALID_VALUE", health.getDetails().get("fallbackReason"));
    }
}
