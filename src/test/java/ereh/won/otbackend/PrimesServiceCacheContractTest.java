package ereh.won.otbackend;

import ereh.won.otbackend.cache.CacheMetrics;
import ereh.won.otbackend.cache.InMemoryPrimeCache;
import ereh.won.otbackend.cache.PrimeCache;
import ereh.won.otbackend.cache.PrimeCacheKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PrimesServiceCacheContractTest {

	@Mock
	private ServiceMetrics serviceMetrics;

	@Mock
	private CacheMetrics cacheMetrics;

	@Test
	void missPathComputesStoresAndReturnsPrime() {
		PrimeCache primeCache = new InMemoryPrimeCache();
		PrimesService primesService = new PrimesService(serviceMetrics, primeCache, cacheMetrics);

		int result = primesService.getPrime(10);

		assertEquals(29, result);
		assertEquals(29, primeCache.get(new PrimeCacheKey(10)).orElseThrow());
		verify(serviceMetrics).recordRequest(PrimesService.GET_PRIME_ENDPOINT);
	}

	@Test
	void hitPathReturnsCachedValueForSamePosition() {
		PrimeCache primeCache = new InMemoryPrimeCache();
		PrimeCacheKey key = new PrimeCacheKey(10);
		primeCache.put(key, 29);
		PrimesService primesService = new PrimesService(serviceMetrics, primeCache, cacheMetrics);

		int result = primesService.getPrime(10);

		assertEquals(29, result);
		assertTrue(primeCache.get(key).isPresent());
		verify(serviceMetrics).recordRequest(PrimesService.GET_PRIME_ENDPOINT);
	}

	@Test
	void invalidPositionThrowsWithoutWritingCache() {
		PrimeCache primeCache = new InMemoryPrimeCache();
		PrimesService primesService = new PrimesService(serviceMetrics, primeCache, cacheMetrics);

		assertThrows(InvalidNumberException.class, () -> primesService.getPrime(0));
		assertTrue(primeCache.get(new PrimeCacheKey(1)).isEmpty());
		verify(serviceMetrics).recordError(PrimesService.GET_PRIME_ENDPOINT, "invalid_input");
		verify(serviceMetrics, never()).recordRequest(PrimesService.GET_PRIME_ENDPOINT);
	}
}
