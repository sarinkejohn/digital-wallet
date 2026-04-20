package dev.sarinkejohn.gateway.limiting;

import dev.sarinkejohn.gateway.config.ChannelProperties;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TierResolver {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final CacheService cacheService;
    private final ChannelProperties channelProperties;

    public TierResolver(ReactiveStringRedisTemplate redisTemplate,
                        CacheService cacheService,
                        ChannelProperties channelProperties) {
        this.redisTemplate = redisTemplate;
        this.cacheService = cacheService;
        this.channelProperties = channelProperties;
    }

    public Mono<String> resolveTier(String clientId) {
        String cachedTier = cacheService.getTier(clientId);
        if (cachedTier != null) {
            return Mono.just(cachedTier);
        }
        // If it's not cached, fetch from Redis asynchronously and populate cache
        return redisTemplate.opsForValue().get("rate_limit_tier:" + clientId)
                .doOnNext(tier -> cacheService.putTier(clientId, tier));
    }

    /**
     * Resolve the rate-limit tier for a specific channel.
     * Channels are mapped to tiers via ChannelProperties (e.g. WEBP→SILVER, SWIFTY→GOLD).
     * Falls back to the client-level tier if no channel mapping exists.
     */
    public Mono<String> resolveChannelTier(String channel, String clientId) {
        String channelTier = channelProperties.getTierForChannel(channel);
        if (channelTier != null) {
            return Mono.just(channelTier);
        }
        // Fallback: resolve by client identity
        return resolveTier(clientId);
    }
}
