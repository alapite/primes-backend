package ereh.won.otbackend.cache;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryPrimeCache implements PrimeCache {
	private final ConcurrentHashMap<PrimeCacheKey, Integer> cacheEntries = new ConcurrentHashMap<>();

	@Override
	public Optional<Integer> get(PrimeCacheKey key) {
		return Optional.ofNullable(cacheEntries.get(key));
	}

	@Override
	public void put(PrimeCacheKey key, int value) {
		cacheEntries.put(key, value);
	}
}
