package ereh.won.otbackend.cache;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Parity tests for in-memory cache backend.
 * Implements PrimeCacheParityContract to verify MEMORY backend satisfies all contract assertions.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestPropertySource(properties = {
	"prime.cache.backend=MEMORY"
})
class PrimeCacheMemoryParityIT implements PrimeCacheParityContract {

	@Autowired
	private PrimeCache primeCache;

	@Override
	public PrimeCache cache() {
		return primeCache;
	}
}
