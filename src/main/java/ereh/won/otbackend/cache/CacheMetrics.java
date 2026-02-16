package ereh.won.otbackend.cache;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

@Component
@Log
public class CacheMetrics {
	private final MeterRegistry registry;
	private final String backendName;

	public CacheMetrics(MeterRegistry registry, PrimeCacheSelection cacheSelection) {
		this.registry = registry;
		this.backendName = cacheSelection.getEffectiveBackend().name().toLowerCase();
		log.info("CacheMetrics initialized for backend: " + backendName);
	}

	public void record(String operation, String outcome) {
		Counter.builder("prime.cache.operations")
				.description("Prime cache operations by backend/outcome")
				.tag("backend", backendName)
				.tag("operation", operation)
				.tag("outcome", outcome)
				.register(registry)
				.increment();
	}
}
