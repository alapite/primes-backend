package ereh.won.otbackend.cache;

import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;

public class RedisPrimeCache implements PrimeCacheBackend {

    private static final String KEY_PREFIX = "prime:cache:";
    private final StringRedisTemplate redisTemplate;

    public RedisPrimeCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Optional<Integer> get(PrimeCacheKey key) {
        String cacheKey = buildKey(key);
        String value = redisTemplate.opsForValue().get(cacheKey);
        if (value == null) {
            return Optional.empty();
        }
        return Optional.of(Integer.parseInt(value));
    }

    @Override
    public void put(PrimeCacheKey key, int value) {
        String cacheKey = buildKey(key);
        redisTemplate.opsForValue().set(cacheKey, String.valueOf(value));
    }

    private String buildKey(PrimeCacheKey key) {
        return KEY_PREFIX + key.position();
    }
}
