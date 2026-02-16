package ereh.won.otbackend.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for concurrent cache access correctness.
 * <p>
 * These tests verify that concurrent same-key operations maintain correctness
 * without requiring deduplication. Duplicate computes and last-write-wins are
 * acceptable as long as the final cached value is correct.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "prime.cache.backend=MEMORY"
})
class PrimeCacheConcurrencyIntegrationTest {

    @Autowired
    private PrimeCache primeCache;

    @Test
	void concurrentWritesToSameKeyPreservesCorrectValue() throws InterruptedException {
		// Given: Multiple threads writing the same correct value to the same key
		PrimeCacheKey key = new PrimeCacheKey(10);
		int correctValue = 29; // 10th prime is 29
		
		int threadCount = 10;
		CountDownLatch latch = new CountDownLatch(threadCount);
		try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
			// When: All threads write the same value concurrently
			for (int i = 0; i < threadCount; i++) {
				executor.submit(() -> {
					primeCache.put(key, correctValue);
					latch.countDown();
				});
			}
			latch.await();
		}
        
        // Then: The value should be correct (last-write-wins with identical value)
        assertTrue(primeCache.get(key).isPresent());
        assertEquals(correctValue, primeCache.get(key).orElseThrow());
    }

    @Test
	void concurrentMixedWritesToSameKeyPreservesCorrectValue() throws InterruptedException {
		// Given: Multiple threads writing potentially different (but correct) values
		PrimeCacheKey key = new PrimeCacheKey(10);
		int correctValue = 29; // 10th prime is 29
		
		int threadCount = 10;
		CountDownLatch startLatch = new CountDownLatch(1);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);
		try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
			// When: All threads start at the same time and write the correct value
			for (int i = 0; i < threadCount; i++) {
				executor.submit(() -> {
					try {
						startLatch.await(); // Wait for all threads to be ready
						primeCache.put(key, correctValue);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					} finally {
						doneLatch.countDown();
					}
				});
			}

			startLatch.countDown(); // Start all threads simultaneously
			doneLatch.await();
		}
        
        // Then: The final value should be correct
        assertTrue(primeCache.get(key).isPresent());
        assertEquals(correctValue, primeCache.get(key).orElseThrow());
    }

    @Test
	void concurrentReadWriteToSameKeyMaintainsConsistency() throws InterruptedException {
		// Given: Cache with an initial value
		PrimeCacheKey key = new PrimeCacheKey(20);
		int initialValue = 71; // 20th prime is 71
		primeCache.put(key, initialValue);
		
		int threadCount = 5;
		CountDownLatch latch = new CountDownLatch(threadCount * 2);
		AtomicInteger readCount = new AtomicInteger(0);
		try (ExecutorService executor = Executors.newFixedThreadPool(threadCount * 2)) {
			// When: Multiple threads read and write concurrently
			for (int i = 0; i < threadCount; i++) {
				// Reader threads
				executor.submit(() -> {
					primeCache.get(key).ifPresent(_ -> readCount.incrementAndGet());
					latch.countDown();
				});

				// Writer threads
				executor.submit(() -> {
					primeCache.put(key, initialValue); // Always write correct value
					latch.countDown();
				});
			}
			latch.await();
		}
        
        // Then: Final value should still be correct and some reads should have succeeded
        assertTrue(primeCache.get(key).isPresent());
        assertEquals(initialValue, primeCache.get(key).orElseThrow());
        assertTrue(readCount.get() > 0, "At least some reads should have succeeded");
    }

    @Test
	void concurrentAccessToDifferentKeysAreIndependent() throws InterruptedException {
        // Given: Multiple keys
        int keyCount = 10;
        PrimeCacheKey[] keys = new PrimeCacheKey[keyCount];
        int[] expectedValues = new int[keyCount];
        
        for (int i = 0; i < keyCount; i++) {
            keys[i] = new PrimeCacheKey(i + 1);
            expectedValues[i] = getNthPrime(i + 1);
            primeCache.put(keys[i], expectedValues[i]);
        }
        
		int threadCount = 20;
		CountDownLatch latch = new CountDownLatch(threadCount);
		try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
			// When: Multiple threads access different keys concurrently
			for (int i = 0; i < threadCount; i++) {
				final int index = i % keyCount;
				executor.submit(() -> {
					primeCache.get(keys[index]);
					latch.countDown();
				});
			}
			latch.await();
		}
        
        // Then: All keys should have their correct values
        for (int i = 0; i < keyCount; i++) {
            assertEquals(expectedValues[i], primeCache.get(keys[i]).orElseThrow(),
                "Key " + (i + 1) + " should have correct value");
        }
    }

    @Test
	void concurrentWritesWithCountingMaintainsCorrectness() throws Exception {
		// Given: A key that will receive concurrent writes
		PrimeCacheKey key = new PrimeCacheKey(5);
		int correctValue = 11; // 5th prime is 11
		
		int threadCount = 50;
		AtomicInteger writeCount = new AtomicInteger(0);
		CountDownLatch doneLatch = new CountDownLatch(threadCount);
		try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {
			// When: Multiple threads write the correct value
			for (int i = 0; i < threadCount; i++) {
				executor.submit(() -> {
					primeCache.put(key, correctValue);
					writeCount.incrementAndGet();
					doneLatch.countDown();
				});
			}
			doneLatch.await();
		}
        
        // Then: Final value should be correct and all writes should have occurred
        assertTrue(primeCache.get(key).isPresent());
        assertEquals(correctValue, primeCache.get(key).orElseThrow());
        assertEquals(threadCount, writeCount.get());
    }

    private int getNthPrime(int n) {
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
