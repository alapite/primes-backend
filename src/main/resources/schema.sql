-- Prime cache table for PostgreSQL backend
CREATE TABLE IF NOT EXISTS prime_cache (
    position INTEGER PRIMARY KEY,
    prime_value INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster lookups (though primary key already covers this)
CREATE INDEX IF NOT EXISTS idx_prime_cache_position ON prime_cache(position);
