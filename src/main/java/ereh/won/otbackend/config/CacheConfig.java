package ereh.won.otbackend.config;

import ereh.won.otbackend.cache.InMemoryPrimeCache;
import ereh.won.otbackend.cache.PostgresPrimeCache;
import ereh.won.otbackend.cache.PrimeCache;
import ereh.won.otbackend.cache.PrimeCacheProperties;
import ereh.won.otbackend.cache.PrimeCacheSelection;
import ereh.won.otbackend.cache.RedisPrimeCache;
import lombok.extern.java.Log;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.DriverManager;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(PrimeCacheProperties.class)
@Log
public class CacheConfig {

    private final PrimeCacheProperties properties;
    private final PrimeCacheSelection cacheSelection;

    public CacheConfig(PrimeCacheProperties properties) {
        this.properties = properties;
        this.cacheSelection = resolveBackend();
    }

    private PrimeCacheSelection resolveBackend() {
        PrimeCacheProperties.Backend configured = properties.getBackend();
        PrimeCacheProperties.Backend effective;
        PrimeCacheSelection.FallbackReason fallbackReason = null;

        // Default to memory if not configured
        if (configured == null) {
            log.warning("No cache backend configured, defaulting to MEMORY");
            effective = PrimeCacheProperties.Backend.MEMORY;
            fallbackReason = PrimeCacheSelection.FallbackReason.NOT_CONFIGURED;
        } else if (configured == PrimeCacheProperties.Backend.MEMORY) {
            effective = PrimeCacheProperties.Backend.MEMORY;
        } else {
            // Try to probe the backend connectivity
            boolean connected = probeBackendConnectivity(configured);
            if (connected) {
                effective = configured;
                log.info("Cache backend '" + configured + "' selected as effective backend");
            } else {
                log.warning("Configured backend '" + configured + "' is unreachable, falling back to MEMORY");
                effective = PrimeCacheProperties.Backend.MEMORY;
                fallbackReason = PrimeCacheSelection.FallbackReason.CONNECTIVITY_FAILED;
            }
        }

        return new PrimeCacheSelection(configured, effective, fallbackReason);
    }

    private boolean probeBackendConnectivity(PrimeCacheProperties.Backend backend) {
        try {
            return switch (backend) {
                case REDIS -> probeRedis();
                case POSTGRES -> probePostgres();
                default -> false;
            };
        } catch (Exception e) {
            log.warning("Failed to probe backend '" + backend + "': " + e.getMessage());
            return false;
        }
    }

    private boolean probeRedis() {
        LettuceConnectionFactory factory = null;
        try {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
            config.setHostName(properties.getRedis().getHost());
            config.setPort(properties.getRedis().getPort());
            factory = new LettuceConnectionFactory(config);
            factory.afterPropertiesSet();
            try (RedisConnection connection = factory.getConnection()) {
                connection.ping();
            }
            return true;
        } catch (Exception e) {
            log.warning("Redis connectivity probe failed: " + e.getMessage());
            return false;
        } finally {
            if (factory != null) {
                factory.destroy();
            }
        }
    }

    private boolean probePostgres() {
        try {
            var pgProperties = properties.getPostgres();
            String url = "jdbc:postgresql://%s:%d/%s"
                    .formatted(pgProperties.getHost(), pgProperties.getPort(), pgProperties.getDatabase());
            Properties connectionProperties = new Properties();
            if (pgProperties.getUsername() != null) {
                connectionProperties.setProperty("user", pgProperties.getUsername());
            }
            if (pgProperties.getPassword() != null) {
                connectionProperties.setProperty("password", pgProperties.getPassword());
            }

            DriverManager.setLoginTimeout(5);
            try (var connection = DriverManager.getConnection(url, connectionProperties);
                 var statement = connection.prepareStatement("SELECT 1");
                 var resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (Exception e) {
            log.warning("PostgreSQL connectivity probe failed: " + e.getMessage());
            return false;
        }
    }

    @Bean
    public PrimeCacheSelection cacheSelection() {
        return cacheSelection;
    }

    @Bean
    @ConditionalOnMissingBean
    public PrimeCache primeCache(RedisConnectionFactory redisConnectionFactory,
                                  JdbcTemplate jdbcTemplate) {
        PrimeCacheProperties.Backend effective = cacheSelection.getEffectiveBackend();
        log.info("Creating PrimeCache with effective backend: " + effective);

        return switch (effective) {
            case REDIS -> new RedisPrimeCache(new StringRedisTemplate(redisConnectionFactory));
            case POSTGRES -> new PostgresPrimeCache(jdbcTemplate);
            default -> new InMemoryPrimeCache();
        };
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(properties.getRedis().getHost());
        config.setPort(properties.getRedis().getPort());
        return new LettuceConnectionFactory(config);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public DataSource dataSource() {
        PrimeCacheProperties.Backend effective = cacheSelection.getEffectiveBackend();

        if (effective == PrimeCacheProperties.Backend.POSTGRES) {
            var pgProperties = properties.getPostgres();

            var dataSource = new PGSimpleDataSource();

            dataSource.setDatabaseName(pgProperties.getDatabase());
            dataSource.setServerNames(new String[]{pgProperties.getHost()});
            dataSource.setPortNumbers(new int[]{pgProperties.getPort()});
            dataSource.setUser(pgProperties.getUsername());
            dataSource.setPassword(pgProperties.getPassword());

            return dataSource;
        }

        // Default embedded H2 for development
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .setName("primecache")
                .build();
    }
}
