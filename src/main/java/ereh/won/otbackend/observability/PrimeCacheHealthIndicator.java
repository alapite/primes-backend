package ereh.won.otbackend.observability;

import ereh.won.otbackend.cache.PrimeCacheSelection;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PrimeCacheHealthIndicator implements HealthIndicator {

    private final PrimeCacheSelection cacheSelection;

    public PrimeCacheHealthIndicator(PrimeCacheSelection cacheSelection) {
        this.cacheSelection = cacheSelection;
    }

	@Override
	public Health health() {
		Map<String, Object> details = new HashMap<>();
		
		details.put("configuredBackend", cacheSelection.getConfiguredBackend().name());
		details.put("effectiveBackend", cacheSelection.getEffectiveBackend().name());

		Health.Builder statusBuilder = Health.up();
		
		if (cacheSelection.isFallback()) {
			statusBuilder = Health.status("DEGRADED");
			details.put("fallbackReason", cacheSelection.getFallbackReason().name());
		}
		
		return statusBuilder.withDetails(details)
				.build();
	}
}
