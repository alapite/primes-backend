package ereh.won.otbackend.cache;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Parity tests for PostgreSQL cache backend.
 * Implements PrimeCacheParityContract to verify POSTGRES backend satisfies all contract assertions.
 *
 * Uses a Testcontainers-managed PostgreSQL instance for deterministic parity validation.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
class PrimeCachePostgresParityIT implements PrimeCacheParityContract {

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("primecache")
			.withUsername("postgres")
			.withPassword("postgres");

	@DynamicPropertySource
	static void registerPostgresProperties(DynamicPropertyRegistry registry) {
		registry.add("prime.cache.backend", () -> "POSTGRES");
		registry.add("prime.cache.postgres.host", POSTGRES::getHost);
		registry.add("prime.cache.postgres.port", POSTGRES::getFirstMappedPort);
		registry.add("prime.cache.postgres.database", POSTGRES::getDatabaseName);
		registry.add("prime.cache.postgres.username", POSTGRES::getUsername);
		registry.add("prime.cache.postgres.password", POSTGRES::getPassword);
	}

	@Autowired
	private PrimeCache primeCache;

	@Autowired
	private PrimeCacheSelection cacheSelection;

	@Test
	void effectiveBackendIsPostgres_NotMemoryFallback() {
		// Assert effective backend is POSTGRES - prevents false-positive parity
		assertEquals(PrimeCacheProperties.Backend.POSTGRES, cacheSelection.getEffectiveBackend(),
				"Effective backend must be POSTGRES, not MEMORY fallback");
		assertFalse(cacheSelection.isFallback(),
				"Should not fallback to MEMORY - PostgreSQL should be reachable");
		assertEquals(PrimeCacheProperties.Backend.POSTGRES, cacheSelection.getConfiguredBackend(),
				"Configured backend should be POSTGRES");
	}

	@Override
	public PrimeCache cache() {
		return primeCache;
	}
}
