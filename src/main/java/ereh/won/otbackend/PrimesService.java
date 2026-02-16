package ereh.won.otbackend;

import ereh.won.otbackend.cache.CacheMetrics;
import ereh.won.otbackend.cache.PrimeCache;
import ereh.won.otbackend.cache.PrimeCacheKey;
import io.micrometer.observation.annotation.Observed;
import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Log
public class PrimesService {
    public static final String GET_PRIME_ENDPOINT = "/api/primes/getPrime";
    private static final String INVALID_INPUT_ERROR_TYPE = "invalid_input";

    private final ServiceMetrics serviceMetrics;
    private final PrimeCache primeCache;
    private final CacheMetrics cacheMetrics;

    public PrimesService(ServiceMetrics serviceMetrics, PrimeCache primeCache, CacheMetrics cacheMetrics) {
        this.serviceMetrics = serviceMetrics;
        this.primeCache = primeCache;
        this.cacheMetrics = cacheMetrics;
    }

    @Observed
    @WithSpan
    public int getPrime(@SpanAttribute("primePosition") int primePosition) {
        if (primePosition < 1) {
            serviceMetrics.recordError(GET_PRIME_ENDPOINT, INVALID_INPUT_ERROR_TYPE);
            log.severe(primePosition + " is an invalid index for a prime");
            throw new InvalidNumberException(primePosition);
        }

        serviceMetrics.recordRequest(GET_PRIME_ENDPOINT);
        Instant requestStart = Instant.now();
        PrimeCacheKey cacheKey = new PrimeCacheKey(primePosition);
        Optional<Integer> cached;
        try {
            cached = primeCache.get(cacheKey);
        } catch (Exception e) {
            log.warning("Failed to read prime at position " + primePosition + " from cache: " + e.getMessage());
            cacheMetrics.record("get", "error");
            cached = Optional.empty();
        }

        if (cached.isPresent()) {
            cacheMetrics.record("get", "hit");
            serviceMetrics.recordResponseTime(GET_PRIME_ENDPOINT, requestStart.until(Instant.now(), ChronoUnit.MILLIS));
            return cached.get();
        }

        cacheMetrics.record("get", "miss");
        int computedPrime = PrimeUtils.calculateNthPrime(primePosition);
        try {
            primeCache.put(cacheKey, computedPrime);
            cacheMetrics.record("put", "success");
        } catch (Exception e) {
            log.warning("Failed to write prime at position " + primePosition + " to cache: " + e.getMessage());
            cacheMetrics.record("put", "error");
        }
        return computedPrime;
    }
}
