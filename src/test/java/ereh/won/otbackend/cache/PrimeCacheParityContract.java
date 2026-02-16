package ereh.won.otbackend.cache;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract interface defining parity tests that all cache backends must satisfy.
 * Implementing classes run these default test methods to verify semantic equivalence.
 */
public interface PrimeCacheParityContract {

	/**
	 * Provides the cache backend instance under test.
	 * Each implementation must supply a properly configured cache.
	 */
	PrimeCache cache();

	@Test
	default void sameInputProducesSameOutput() {
		// Contract: put(key, value) then get(key) returns same value
		PrimeCacheKey key = new PrimeCacheKey(10);
		cache().put(key, 29);
		assertEquals(29, cache().get(key).orElseThrow(), "Cache should return the same value that was stored");
	}

	@Test
	default void differentKeysIndependent() {
		// Contract: multiple keys don't interfere with each other
		PrimeCacheKey key10 = new PrimeCacheKey(10);
		PrimeCacheKey key20 = new PrimeCacheKey(20);
		PrimeCacheKey key30 = new PrimeCacheKey(30);

		cache().put(key10, 29);
		cache().put(key20, 71);
		cache().put(key30, 113);

		assertEquals(29, cache().get(key10).orElseThrow(), "Key 10 should retain its value");
		assertEquals(71, cache().get(key20).orElseThrow(), "Key 20 should retain its value");
		assertEquals(113, cache().get(key30).orElseThrow(), "Key 30 should retain its value");
	}

	@Test
	default void emptyCacheReturnsEmpty() {
		// Contract: get on missing key returns empty Optional
		PrimeCacheKey key = new PrimeCacheKey(999);
		Optional<Integer> result = cache().get(key);
		assertFalse(result.isPresent(), "Cache should return empty Optional for missing key");
	}

	@Test
	default void valueUpdateOverwritesPrevious() {
		// Contract: putting same key again overwrites previous value
		PrimeCacheKey key = new PrimeCacheKey(10);
		cache().put(key, 29);
		cache().put(key, 99); // Overwrite with different value

		assertEquals(99, cache().get(key).orElseThrow(), "Cache should return the most recently stored value");
	}

	@Test
	default void concurrentWritesCorrect() throws Exception {
		// Contract: multiple puts for same key don't corrupt (last write wins is acceptable for v1)
		PrimeCacheKey key = new PrimeCacheKey(10);
		try (ExecutorService executor = Executors.newFixedThreadPool(3)) {
			Future<?> future1 = executor.submit(() -> {
				for (int i = 0; i < 100; i++) {
					cache().put(key, i);
				}
			});

			Future<?> future2 = executor.submit(() -> {
				for (int i = 0; i < 100; i++) {
					cache().put(key, i + 100);
				}
			});

			Future<?> future3 = executor.submit(() -> {
				for (int i = 0; i < 100; i++) {
					cache().put(key, i + 200);
				}
			});

			future1.get();
			future2.get();
			future3.get();
		}

		// Verify that some value was stored and is retrievable
		Optional<Integer> result = cache().get(key);
		assertTrue(result.isPresent(), "Cache should have a value after concurrent writes");
		// The exact value depends on timing, but it should be within our write range
		int value = result.get();
		assertTrue(value >= 0 && value < 300, "Value should be from our write range: " + value);
	}
}
