package ereh.won.otbackend;

import ereh.won.otbackend.cache.CacheMetrics;
import ereh.won.otbackend.cache.PostgresPrimeCache;
import ereh.won.otbackend.cache.PrimeCache;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;

/**
 * Failure parity integration test for POSTGRES-backed PrimesService.
 * <p>
 * Implements PrimesServiceFailureParityContract to verify that when the POSTGRES
 * cache backend experiences failures (or is unavailable), the PrimesService
 * exhibits the same graceful degradation behavior as other backends.
 * <p>
 * Uses Testcontainers PostgreSQL to run real PostgreSQL for production-accurate testing.
 * <p>
 * This test addresses OPS-03: "Cache failure paths behave equivalently across backends."
 */
@Testcontainers
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(properties = {
	"prime.cache.backend=POSTGRES"
})
class PrimesServicePostgresFailureParityIT implements PrimesServiceFailureParityContract {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("testdb")
			.withUsername("test")
			.withPassword("test");

	@Autowired
	private PrimeCache primeCache;

	@Autowired
	private CacheMetrics cacheMetrics;

	@Autowired
	private ServiceMetrics serviceMetrics;

	@TestConfiguration
	static class PostgresCacheTestConfig {

		@Bean
		@Primary
		DataSource testDataSource() {
			org.postgresql.ds.PGSimpleDataSource dataSource = new org.postgresql.ds.PGSimpleDataSource();
			dataSource.setUrl(postgres.getJdbcUrl());
			dataSource.setUser(postgres.getUsername());
			dataSource.setPassword(postgres.getPassword());
			return dataSource;
		}

		@Bean
		@Primary
		JdbcTemplate testJdbcTemplate(DataSource testDataSource) {
			return new JdbcTemplate(testDataSource);
		}

		@Bean
		@Primary
		PrimeCache testPrimeCache(JdbcTemplate testJdbcTemplate) {
			return new PostgresPrimeCache(testJdbcTemplate);
		}
	}

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.sql.init.mode", () -> "always");
	}

	@Override
	public PrimesService primesService() {
		return new PrimesService(serviceMetrics, primeCache, cacheMetrics);
	}
}
