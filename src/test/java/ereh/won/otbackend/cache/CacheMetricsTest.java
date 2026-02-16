package ereh.won.otbackend.cache;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CacheMetricsTest {

	@Test
	void recordsMetricsWithEffectiveBackendTag() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();
		PrimeCacheSelection cacheSelection = mock(PrimeCacheSelection.class);
		when(cacheSelection.getEffectiveBackend()).thenReturn(PrimeCacheProperties.Backend.MEMORY);

		CacheMetrics cacheMetrics = new CacheMetrics(registry, cacheSelection);
		cacheMetrics.record("get", "hit");

		double count = registry.counter(
				"prime.cache.operations",
				"backend", "memory",
				"operation", "get",
				"outcome", "hit"
		).count();
		assertEquals(1.0d, count);
	}
}
