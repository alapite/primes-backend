package ereh.won.otbackend.cache;

public record PrimeCacheKey(Integer position) {
	public PrimeCacheKey {
		if (position == null) {
			throw new IllegalArgumentException("Prime cache position cannot be null");
		}
		if (position < 1) {
			throw new IllegalArgumentException("Prime cache position must be positive");
		}
	}
}
