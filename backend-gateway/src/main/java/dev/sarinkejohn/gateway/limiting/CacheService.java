package dev.sarinkejohn.gateway.limiting;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import java.util.concurrent.TimeUnit;

@Service
public class CacheService {

    private final Cache<String, String> tierCache;
    private final Cache<String, Integer> localRateLimitCounters;

    public CacheService() {
        this.tierCache = Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .maximumSize(10000)
                .build();
                
        this.localRateLimitCounters = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES) // 1 minute rolling window manually handled
                .maximumSize(10000)
                .build();
    }

    public String getTier(String clientId) {
        return tierCache.getIfPresent(clientId);
    }

    public void putTier(String clientId, String tier) {
        tierCache.put(clientId, tier);
    }
    
    // For our Local Fallback implementation
    public boolean isLocalAllowed(String clientId, int maxRequestsPerMinute) {
        Integer currentCount = localRateLimitCounters.get(clientId, k -> 0);
        if (currentCount != null && currentCount >= maxRequestsPerMinute) {
            return false;
        }
        localRateLimitCounters.put(clientId, currentCount == null ? 1 : currentCount + 1);
        return true;
    }
}
