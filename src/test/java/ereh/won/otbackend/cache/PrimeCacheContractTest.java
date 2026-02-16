package ereh.won.otbackend.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrimeCacheContractTest {

	@Test
	void storesAndReadsValueUsingDeterministicKeySemantics() {
		PrimeCache cache = new InMemoryPrimeCache();
		cache.put(new PrimeCacheKey(10), 29);

		assertTrue(cache.get(new PrimeCacheKey(10)).isPresent());
		assertEquals(29, cache.get(new PrimeCacheKey(10)).orElseThrow());
	}

	@Test
	void keepsEntriesIsolatedAcrossDifferentPositions() {
		PrimeCache cache = new InMemoryPrimeCache();
		cache.put(new PrimeCacheKey(5), 11);
		cache.put(new PrimeCacheKey(6), 13);

		assertEquals(11, cache.get(new PrimeCacheKey(5)).orElseThrow());
		assertEquals(13, cache.get(new PrimeCacheKey(6)).orElseThrow());
	}

	@Test
	void keepsStoredPrimeReadableAcrossRepeatedReads() {
		PrimeCache cache = new InMemoryPrimeCache();
		PrimeCacheKey key = new PrimeCacheKey(30);
		cache.put(key, 113);

		assertEquals(113, cache.get(key).orElseThrow());
		assertEquals(113, cache.get(key).orElseThrow());
		assertEquals(113, cache.get(key).orElseThrow());
	}

	@Test
	void rejectsInvalidCacheKeyConstruction() {
		assertThrows(IllegalArgumentException.class, () -> new PrimeCacheKey(null));
		assertThrows(IllegalArgumentException.class, () -> new PrimeCacheKey(0));
		assertThrows(IllegalArgumentException.class, () -> new PrimeCacheKey(-1));
	}
}
