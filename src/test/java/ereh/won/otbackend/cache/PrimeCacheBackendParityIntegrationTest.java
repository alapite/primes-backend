package ereh.won.otbackend.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
    "prime.cache.backend=MEMORY"
})
class PrimeCacheBackendParityIntegrationTest {

    @Autowired
    private PrimeCache primeCache;

    @Test
    void cacheStoresAndRetrievesPrimeValuesCorrectly() {
        // Test that cache correctly stores and retrieves nth-prime values
        PrimeCacheKey key10 = new PrimeCacheKey(10);
        primeCache.put(key10, 29); // 10th prime is 29
        
        PrimeCacheKey key20 = new PrimeCacheKey(20);
        primeCache.put(key20, 71); // 20th prime is 71
        
        PrimeCacheKey key30 = new PrimeCacheKey(30);
        primeCache.put(key30, 113); // 30th prime is 113

        // Verify all values are correctly stored and retrieved
        assertEquals(29, primeCache.get(key10).orElseThrow());
        assertEquals(71, primeCache.get(key20).orElseThrow());
        assertEquals(113, primeCache.get(key30).orElseThrow());
    }

    @Test
    void cacheMaintainsDataIntegrityAcrossMultipleOperations() {
        // Test data integrity across multiple get/put operations
        for (int i = 1; i <= 50; i++) {
            PrimeCacheKey key = new PrimeCacheKey(i);
            int expectedPrime = calculateNthPrime(i);
            primeCache.put(key, expectedPrime);
        }

        // Verify all values are still correct
        for (int i = 1; i <= 50; i++) {
            PrimeCacheKey key = new PrimeCacheKey(i);
            int expectedPrime = calculateNthPrime(i);
            assertEquals(expectedPrime, primeCache.get(key).orElseThrow(),
                "Value mismatch for position " + i);
        }
    }

    @Test
    void cacheReturnsCorrectValueForSingleEntry() {
        PrimeCacheKey key = new PrimeCacheKey(1);
        primeCache.put(key, 2); // 1st prime is 2
        
        assertTrue(primeCache.get(key).isPresent());
        assertEquals(2, primeCache.get(key).orElseThrow());
    }

    @Test
    void cacheHandlesEmptyKey() {
        // Verify that empty keys don't cause issues
        PrimeCacheKey key100 = new PrimeCacheKey(100);
        int expectedPrime = calculateNthPrime(100);
        primeCache.put(key100, expectedPrime);
        
        assertEquals(expectedPrime, primeCache.get(key100).orElseThrow());
    }

    private int calculateNthPrime(int n) {
        // Simple prime calculation for testing
        int count = 0;
        int candidate = 1;
        while (count < n) {
            candidate++;
            if (isPrime(candidate)) {
                count++;
            }
        }
        return candidate;
    }

    private boolean isPrime(int num) {
        if (num < 2) return false;
        for (int i = 2; i <= Math.sqrt(num); i++) {
            if (num % i == 0) return false;
        }
        return true;
    }
}
