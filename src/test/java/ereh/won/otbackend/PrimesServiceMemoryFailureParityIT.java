package ereh.won.otbackend;

import ereh.won.otbackend.cache.CacheMetrics;
import ereh.won.otbackend.cache.PrimeCache;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Failure parity integration test for MEMORY-backed PrimesService.
 * 
 * Implements PrimesServiceFailureParityContract to verify that when the MEMORY
 * cache backend experiences failures (or is unavailable), the PrimesService
 * exhibits the same graceful degradation behavior as other backends.
 * 
 * This test addresses OPS-03: "Cache failure paths behave equivalently across backends."
 * 
 * Configuration: Uses in-memory cache backend.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(properties = {
	"prime.cache.backend=MEMORY"
})
class PrimesServiceMemoryFailureParityIT implements PrimesServiceFailureParityContract {

	@Autowired
	private PrimeCache primeCache;

	@Autowired
	private CacheMetrics cacheMetrics;

	@Autowired
	private ServiceMetrics serviceMetrics;

	@Autowired
	private OpenTelemetry openTelemetry;

	@Override
	public PrimesService primesService() {
		return new PrimesService(serviceMetrics, primeCache, cacheMetrics, openTelemetry);
	}
}
