package ereh.won.otbackend.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest
@TestPropertySource(properties = {
    "prime.cache.backend=MEMORY"
})
class PrimeCacheBackendSelectionIntegrationTest {

    @Autowired
    private PrimeCacheSelection cacheSelection;

    @Autowired
    private PrimeCache primeCache;

    @Test
    void memoryBackendSelectedWhenExplicitlyConfigured() {
        assertNotNull(cacheSelection);
        assertEquals(PrimeCacheProperties.Backend.MEMORY, cacheSelection.getConfiguredBackend());
        assertEquals(PrimeCacheProperties.Backend.MEMORY, cacheSelection.getEffectiveBackend());
        assertNull(cacheSelection.getFallbackReason(), "No fallback expected when memory is explicitly configured");
        assertInstanceOf(InMemoryPrimeCache.class, primeCache, "Cache instance should be InMemoryPrimeCache");
    }

    @Test
    void cacheStoresAndRetrievesValuesWithMemoryBackend() {
        PrimeCacheKey key = new PrimeCacheKey(10);
        primeCache.put(key, 29);
        
        assertTrue(primeCache.get(key).isPresent());
        assertEquals(29, primeCache.get(key).orElseThrow());
    }

    @Test
    void fallbackReasonIsNullWhenNoFallbackOccurred() {
        assertNull(cacheSelection.getFallbackReason());
        assertFalse(cacheSelection.isFallback());
    }
}
