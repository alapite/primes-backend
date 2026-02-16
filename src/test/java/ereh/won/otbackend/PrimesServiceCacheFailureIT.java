package ereh.won.otbackend;

import ereh.won.otbackend.cache.CacheMetrics;
import ereh.won.otbackend.cache.PrimeCache;
import ereh.won.otbackend.cache.PrimeCacheKey;
import io.opentelemetry.api.OpenTelemetry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Integration test proving graceful degradation at the service level when cache fails.
 *
 * This verifies OPS-01 (resilience) at the integration level: service returns correct
 * computed primes even when cache operations fail.
 */
@ExtendWith(MockitoExtension.class)
class PrimesServiceCacheFailureIT {

	private PrimesService primesService;

	@Mock
	private PrimeCache primeCache;

	@Mock
	private CacheMetrics cacheMetrics;

	@Mock
	private ServiceMetrics serviceMetrics;

	@Mock
	private OpenTelemetry openTelemetry;

	@BeforeEach
	void setUp() {
		// Initialize service with mocked dependencies
		primesService = new PrimesService(serviceMetrics, primeCache, cacheMetrics, openTelemetry);
	}

	@Test
	void serviceReturnsComputedPrimeWhenCacheGetThrows() {
		// Given: Cache throws exception on get (simulating cache read failure)
		doThrow(new RuntimeException("Cache unavailable")).when(primeCache).get(any(PrimeCacheKey.class));

		// When: Requesting a prime value
		int result = primesService.getPrime(10);

		// Then: Service computes and returns the correct prime (29 is the 10th prime)
		assertEquals(29, result, "Service should return computed prime when cache.get() fails");
	}

	@Test
	void serviceReturnsComputedPrimeWhenCachePutThrows() {
		// Given: Cache throws exception on put (simulating cache write failure)
		doThrow(new RuntimeException("Cache unavailable")).when(primeCache).put(any(PrimeCacheKey.class), any(Integer.class));

		// When: Requesting a prime value
		int result = primesService.getPrime(20);

		// Then: Service computes and returns the correct prime (71 is the 20th prime)
		assertEquals(71, result, "Service should return computed prime when cache.put() fails");
	}

	@Test
	void serviceReturnsComputedPrimeWhenCacheReturnsEmpty() {
		// Given: Cache returns empty (cache miss or degraded behavior)
		when(primeCache.get(any(PrimeCacheKey.class))).thenReturn(Optional.empty());

		// When: Requesting a prime value
		int result = primesService.getPrime(30);

		// Then: Service computes and returns the correct prime (113 is the 30th prime)
		assertEquals(113, result, "Service should return computed prime when cache returns empty");
	}

	@Test
	void serviceNoExceptionPropagatesToCaller() {
		// Given: Cache throws exception on both get and put
		RuntimeException cacheException = new RuntimeException("Cache unavailable");
		doThrow(cacheException).when(primeCache).get(any(PrimeCacheKey.class));
		doThrow(cacheException).when(primeCache).put(any(PrimeCacheKey.class), any(Integer.class));

		// When: Requesting a prime value
		// Then: No exception should propagate to caller - service handles gracefully
		int result = primesService.getPrime(5);
		assertEquals(11, result, "Service should handle cache failures gracefully");
	}

	@Test
	void serviceReturnsCorrectPrimeForFirstPosition() {
		// Edge case: First prime (position 1)
		when(primeCache.get(any(PrimeCacheKey.class))).thenReturn(Optional.empty());

		int result = primesService.getPrime(1);
		assertEquals(2, result, "Service should return first prime correctly");
	}

	@Test
	void serviceReturnsCorrectPrimeForLargerPosition() {
		// Edge case: Larger prime position
		when(primeCache.get(any(PrimeCacheKey.class))).thenReturn(Optional.empty());

		int result = primesService.getPrime(100);
		assertEquals(541, result, "Service should return 100th prime correctly");
	}
}
