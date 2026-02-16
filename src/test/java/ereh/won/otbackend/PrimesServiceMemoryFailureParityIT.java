package ereh.won.otbackend;

import ereh.won.otbackend.cache.CacheMetrics;
import ereh.won.otbackend.cache.PrimeCache;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Failure parity integration test for MEMORY-backed PrimesService.
 * <p>
 * Implements PrimesServiceFailureParityContract to verify that when the MEMORY
 * cache backend experiences failures (or is unavailable), the PrimesService
 * exhibits the same graceful degradation behavior as other backends.
 * <p>
 * This test addresses OPS-03: "Cache failure paths behave equivalently across backends."
 * <p>
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

	@Override
	public PrimesService primesService() {
		return new PrimesService(serviceMetrics, primeCache, cacheMetrics);
	}
}
