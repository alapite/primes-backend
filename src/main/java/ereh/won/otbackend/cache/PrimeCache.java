package ereh.won.otbackend.cache;

import java.util.Optional;

public interface PrimeCache {
	Optional<Integer> get(PrimeCacheKey key);

	void put(PrimeCacheKey key, int value);
}
