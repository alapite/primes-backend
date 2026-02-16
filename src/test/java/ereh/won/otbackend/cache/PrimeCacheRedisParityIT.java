package ereh.won.otbackend.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Parity tests for Redis cache backend.
 * Implements PrimeCacheParityContract to verify REDIS backend satisfies all contract assertions.
 *
 * Uses a Testcontainers-managed Redis instance for deterministic parity validation.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PrimeCacheRedisParityIT implements PrimeCacheParityContract {

	private static final int REDIS_PORT = 6379;

	@Container
	private static final GenericContainer<?> REDIS =
			new GenericContainer<>("redis:7.2-alpine").withExposedPorts(REDIS_PORT);

	@DynamicPropertySource
	static void registerRedisProperties(DynamicPropertyRegistry registry) {
		registry.add("prime.cache.backend", () -> "REDIS");
		registry.add("prime.cache.redis.host", REDIS::getHost);
		registry.add("prime.cache.redis.port", () -> REDIS.getMappedPort(REDIS_PORT));
	}

	@Autowired
	private PrimeCache primeCache;

	@Autowired
	private PrimeCacheSelection cacheSelection;

	@Test
	void effectiveBackendIsRedis_NotMemoryFallback() {
		// Assert effective backend is REDIS - prevents false-positive parity
		assertEquals(PrimeCacheProperties.Backend.REDIS, cacheSelection.getEffectiveBackend(),
				"Effective backend must be REDIS, not MEMORY fallback");
		assertFalse(cacheSelection.isFallback(),
				"Should not fallback to MEMORY - Redis should be reachable");
		assertEquals(PrimeCacheProperties.Backend.REDIS, cacheSelection.getConfiguredBackend(),
				"Configured backend should be REDIS");
	}

	@Override
	public PrimeCache cache() {
		return primeCache;
	}
}
