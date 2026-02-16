package ereh.won.otbackend.observability;

import ereh.won.otbackend.cache.PrimeCacheSelection;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class PrimeCacheInfoContributor implements InfoContributor {

    private final PrimeCacheSelection cacheSelection;

    public PrimeCacheInfoContributor(PrimeCacheSelection cacheSelection) {
        this.cacheSelection = cacheSelection;
    }

    @Override
    public void contribute(Info.Builder builder) {
        Map<String, Object> cacheInfo = new HashMap<>();
        
        cacheInfo.put("configuredBackend", cacheSelection.getConfiguredBackend().name());
        cacheInfo.put("effectiveBackend", cacheSelection.getEffectiveBackend().name());
        
        if (cacheSelection.isFallback()) {
            cacheInfo.put("fallbackReason", cacheSelection.getFallbackReason().name());
        }
        
        builder.withDetail("primeCache", cacheInfo);
    }
}
