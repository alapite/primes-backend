package ereh.won.otbackend;

import ereh.won.otbackend.cache.CacheMetrics;
import ereh.won.otbackend.cache.PrimeCache;
import ereh.won.otbackend.cache.PrimeCacheKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrimesServiceFailureParityTest {

	private PrimesService primesService;

	@Mock
	private PrimeCache primeCache;

	@Mock
	private CacheMetrics cacheMetrics;

	@Mock
	private ServiceMetrics serviceMetrics;

	@BeforeEach
	void setUp() {
		primesService = new PrimesService(serviceMetrics, primeCache, cacheMetrics);
	}

	@ParameterizedTest(name = "{index}: {0} read failure still returns correct prime")
	@MethodSource("cacheReadFailureCases")
	void serviceReturnsComputedPrimeWhenCacheReadThrows(String backend, String message, int position, int expectedPrime) {
		doThrow(new RuntimeException(message)).when(primeCache).get(any(PrimeCacheKey.class));

		int result = primesService.getPrime(position);

		assertEquals(expectedPrime, result, "Service should compute prime when " + backend + " read fails");
	}

	@ParameterizedTest(name = "{index}: {0} write failure still returns correct prime")
	@MethodSource("cacheWriteFailureCases")
	void serviceReturnsComputedPrimeWhenCacheWriteThrows(String backend, int position, int expectedPrime) {
		when(primeCache.get(any(PrimeCacheKey.class))).thenReturn(Optional.empty());
		doThrow(new RuntimeException(backend + " write unavailable"))
				.when(primeCache).put(any(PrimeCacheKey.class), any(Integer.class));

		int result = primesService.getPrime(position);

		assertEquals(expectedPrime, result, "Service should compute prime when " + backend + " write fails");
	}

	@Test
	void serviceReturnsComputedPrimeWhenCacheReturnsEmpty() {
		when(primeCache.get(any(PrimeCacheKey.class))).thenReturn(Optional.empty());

		int result = primesService.getPrime(30);

		assertEquals(113, result);
	}

	@Test
	void serviceNoExceptionPropagatesToCallerWhenBothCacheOperationsThrow() {
		RuntimeException cacheException = new RuntimeException("Cache unavailable");
		doThrow(cacheException).when(primeCache).get(any(PrimeCacheKey.class));
		doThrow(cacheException).when(primeCache).put(any(PrimeCacheKey.class), any(Integer.class));

		int result = primesService.getPrime(5);

		assertEquals(11, result);
	}

	@Test
	void serviceReturnsCorrectPrimeForFirstPosition() {
		when(primeCache.get(any(PrimeCacheKey.class))).thenReturn(Optional.empty());

		int result = primesService.getPrime(1);

		assertEquals(2, result);
	}

	@Test
	void serviceReturnsCorrectPrimeForLargerPosition() {
		when(primeCache.get(any(PrimeCacheKey.class))).thenReturn(Optional.empty());

		int result = primesService.getPrime(100);

		assertEquals(541, result);
	}

	private static Stream<Arguments> cacheReadFailureCases() {
		return Stream.of(
				Arguments.of("MEMORY", "MEMORY backend unavailable", 10, 29),
				Arguments.of("REDIS", "REDIS connection refused", 20, 71),
				Arguments.of("POSTGRES", "POSTGRES connection timeout", 30, 113)
		);
	}

	private static Stream<Arguments> cacheWriteFailureCases() {
		return Stream.of(
				Arguments.of("MEMORY", 10, 29),
				Arguments.of("REDIS", 20, 71),
				Arguments.of("POSTGRES", 50, 229)
		);
	}
}
