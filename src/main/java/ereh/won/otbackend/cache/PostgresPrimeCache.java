package ereh.won.otbackend.cache;

import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Optional;

public class PostgresPrimeCache implements PrimeCacheBackend {

    private static final String TABLE_NAME = "prime_cache";
    private final JdbcTemplate jdbcTemplate;

    public PostgresPrimeCache(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        initializeTable();
    }

    private void initializeTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS %s (
                    position INTEGER PRIMARY KEY,
                    prime_value INTEGER NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                """.formatted(TABLE_NAME);
        jdbcTemplate.execute(sql);
    }

    @Override
    public Optional<Integer> get(PrimeCacheKey key) {
        String sql = "SELECT prime_value FROM " + TABLE_NAME + " WHERE position = ?";
        try {
            Integer value = jdbcTemplate.queryForObject(sql, Integer.class, key.position());
            return Optional.ofNullable(value);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public void put(PrimeCacheKey key, int value) {
        String sql = """
                INSERT INTO %s (position, prime_value, created_at, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT (position)
                DO UPDATE SET prime_value = EXCLUDED.prime_value, updated_at = CURRENT_TIMESTAMP
                """.formatted(TABLE_NAME);
        jdbcTemplate.update(sql, key.position(), value);
    }
}
