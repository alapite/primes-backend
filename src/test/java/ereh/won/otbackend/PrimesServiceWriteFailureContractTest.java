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
import static org.mockito.Mockito.verify;

/**
 * Service contract test verifying that PrimesService correctly handles cache write failures.
 * <p>
 * According to the concurrency behaviour contract, API responses should still return the
 * correct prime even if a cache write fails during valid request processing.
 */
@ExtendWith(MockitoExtension.class)
class PrimesServiceWriteFailureContractTest {

    @Mock
    private ServiceMetrics serviceMetrics;

    @Mock
    private CacheMetrics cacheMetrics;

    /**
     * A PrimeCache implementation that throws on put operations.
     * Simulates a backend failure (e.g., Redis/PostgreSQL connection lost).
     */
    static class FailingCache implements PrimeCache {
        private final PrimeCache delegate = new InMemoryPrimeCache();
        
        @Override
        public java.util.Optional<Integer> get(PrimeCacheKey key) {
            return delegate.get(key);
        }
        
        @Override
        public void put(PrimeCacheKey key, int value) {
            throw new RuntimeException("Simulated cache write failure");
        }
    }

    @Test
    void getPrimeReturnsComputedValueWhenCachePutFails() {
        // Given: A cache that fails on put operations
        PrimeCache failingCache = new FailingCache();
        PrimesService primesService = new PrimesService(serviceMetrics, failingCache, cacheMetrics);

        // When: Requesting a prime that isn't cached
        int result = primesService.getPrime(10);

        // Then: The computed prime should be returned (not an error)
        assertEquals(29, result, "Should return correct computed prime even when cache write fails");
        verify(serviceMetrics).recordRequest(PrimesService.GET_PRIME_ENDPOINT);
    }

    @Test
    void getPrimeReturnsCachedValueOnHitPathRegardlessOfWriteFailure() {
        // Given: A cache with a value pre-populated (hit path)
        PrimeCache cacheWithValue = new InMemoryPrimeCache();
        cacheWithValue.put(new PrimeCacheKey(10), 29);
        PrimesService primesService = new PrimesService(serviceMetrics, cacheWithValue, cacheMetrics);

        // When: Requesting a prime that is cached (hit path - no put will occur)
        int result = primesService.getPrime(10);

        // Then: The cached prime should be returned
        assertEquals(29, result);
        verify(serviceMetrics).recordRequest(PrimesService.GET_PRIME_ENDPOINT);
    }

    @Test
    void getPrimeThrowsForInvalidInputRegardlessOfCache() {
        // Given: A failing cache
        PrimeCache failingCache = new FailingCache();
        PrimesService primesService = new PrimesService(serviceMetrics, failingCache, cacheMetrics);

        // When: Requesting a prime with invalid input
        // Then: Invalid input error should be thrown and reported with bounded metric tags
        assertThrows(InvalidNumberException.class, () -> primesService.getPrime(0));
        verify(serviceMetrics).recordError(PrimesService.GET_PRIME_ENDPOINT, "invalid_input");
    }

    @Test
    void cacheWriteFailureDoesNotAffectSubsequentSuccessfulRequests() {
        // Given: A cache that fails on first write
        PrimeCache failingCache = new FailingCache();
        PrimesService primesService = new PrimesService(serviceMetrics, failingCache, cacheMetrics);

        // When: First request fails to write to cache
        int firstResult = primesService.getPrime(10);
        
        // Then: The computed value should still be returned
        assertEquals(29, firstResult);
        
        // When: Second request (the cache has no value due to first failure)
        // Note: Since cache.put failed, the value won't be cached
        // So this will also compute and try to write (which will fail again)
        int secondResult = primesService.getPrime(10);
        
        // Then: The computed value should still be returned correctly
        assertEquals(29, secondResult);
    }

    @Test
    void getPrimeMultiplePositionsWithCacheWriteFailures() {
        // Given: A failing cache
        PrimeCache failingCache = new FailingCache();
        PrimesService primesService = new PrimesService(serviceMetrics, failingCache, cacheMetrics);

        // When/Then: Multiple different prime positions should all return correct computed values
        assertEquals(2, primesService.getPrime(1));   // 1st prime: 2
        assertEquals(3, primesService.getPrime(2));   // 2nd prime: 3
        assertEquals(5, primesService.getPrime(3));   // 3rd prime: 5
        assertEquals(7, primesService.getPrime(4));   // 4th prime: 7
        assertEquals(11, primesService.getPrime(5));  // 5th prime: 11
        assertEquals(13, primesService.getPrime(6));  // 6th prime: 13
    }
}
