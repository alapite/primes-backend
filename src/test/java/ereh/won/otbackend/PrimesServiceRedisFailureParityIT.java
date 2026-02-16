package ereh.won.otbackend;

import ereh.won.otbackend.cache.CacheMetrics;
import ereh.won.otbackend.cache.PrimeCache;
import ereh.won.otbackend.cache.PrimeCacheProperties;
import ereh.won.otbackend.cache.PrimeCacheSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Failure parity integration test for REDIS-backed PrimesService.
 * <p>
 * Implements PrimesServiceFailureParityContract to verify that when the REDIS
 * cache backend experiences failures (or is unavailable), the PrimesService
 * exhibits the same graceful degradation behavior as other backends.
 * <p>
 * Uses Testcontainers Redis to run real Redis for production-accurate testing.
 * Asserts effective backend is REDIS to prevent false-positive results.
 * <p>
 * This test addresses OPS-03: "Cache failure paths behave equivalently across backends."
 */
@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(properties = {
	"prime.cache.backend=REDIS"
})
class PrimesServiceRedisFailureParityIT implements PrimesServiceFailureParityContract {

	@Container
	static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
			.withExposedPorts(6379);

	@Autowired
	private PrimeCache primeCache;

	@Autowired
	private CacheMetrics cacheMetrics;

	@Autowired
	private ServiceMetrics serviceMetrics;

	@Autowired
	private PrimeCacheSelection cacheSelection;

	/**
	 * Assert effective backend is REDIS - prevents false-positive failure parity.
	 */
	@Test
	void effectiveBackendIsRedis_NotMemoryFallback() {
		// Assert effective backend is REDIS - prevents false-positive parity
		assertEquals(PrimeCacheProperties.Backend.REDIS, cacheSelection.getEffectiveBackend(),
				"Effective backend must be REDIS, not MEMORY fallback");
		assertFalse(cacheSelection.isFallback(),
				"Should not fallback to MEMORY - Redis container should be reachable");
		assertEquals(PrimeCacheProperties.Backend.REDIS, cacheSelection.getConfiguredBackend(),
				"Configured backend should be REDIS");
	}

	@TestConfiguration
	static class RedisCacheTestConfig {

		@Bean
		@Primary
		RedisConnectionFactory testRedisConnectionFactory() {
			// Create connection factory pointing to Testcontainers Redis
			org.springframework.data.redis.connection.RedisStandaloneConfiguration config =
					new org.springframework.data.redis.connection.RedisStandaloneConfiguration();
			config.setHostName(redis.getHost());
			config.setPort(redis.getMappedPort(6379));
			return new LettuceConnectionFactory(config);
		}

		@Bean
		@Primary
		StringRedisTemplate testStringRedisTemplate(RedisConnectionFactory connectionFactory) {
			return new StringRedisTemplate(connectionFactory);
		}
	}

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
	}

	@Override
	public PrimesService primesService() {
		return new PrimesService(serviceMetrics, primeCache, cacheMetrics);
	}
}
