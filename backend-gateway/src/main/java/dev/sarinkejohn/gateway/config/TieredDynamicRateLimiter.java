package dev.sarinkejohn.gateway.config;

import dev.sarinkejohn.gateway.limiting.CacheService;
import dev.sarinkejohn.gateway.limiting.TierResolver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.filter.ratelimit.RateLimiter;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TieredDynamicRateLimiter implements RateLimiter<RedisRateLimiter.Config> {

    private static final Logger log = LoggerFactory.getLogger(TieredDynamicRateLimiter.class);

    private final Map<String, RedisRateLimiter> limiters = new HashMap<>();
    private final String defaultPackage;
    private final TierResolver tierResolver;
    private final CacheService cacheService;
    private final ReactiveCircuitBreaker circuitBreaker;

    private final Counter allowedRequestsCounter;
    private final Counter blockedRequestsCounter;

    public TieredDynamicRateLimiter(
            ReactiveStringRedisTemplate redisTemplate,
            RedisScript<List<Long>> redisScript,
            ConfigurationService configurationService,
            RateLimitProperties properties,
            String defaultPackage,
            TierResolver tierResolver,
            CacheService cacheService,
            ReactiveCircuitBreakerFactory circuitBreakerFactory,
            MeterRegistry meterRegistry) {
        
        this.defaultPackage = defaultPackage != null ? defaultPackage.toUpperCase() : "SILVER";
        this.tierResolver = tierResolver;
        this.cacheService = cacheService;
        
        // Resilience4j circuit breaker instance 
        this.circuitBreaker = circuitBreakerFactory.create("redisRateLimiter");

        // Micrometer Prometheus Metrics
        this.allowedRequestsCounter = meterRegistry.counter("rate_limit_allowed_total");
        this.blockedRequestsCounter = meterRegistry.counter("rate_limit_blocked_total");

        if (properties.getPlans() != null) {
            properties.getPlans().forEach((pkg, plan) -> {
                RedisRateLimiter limiter = new RedisRateLimiter(redisTemplate, redisScript, configurationService);
                RedisRateLimiter.Config config = new RedisRateLimiter.Config();
                config.setReplenishRate(plan.getReplenishRate());
                config.setBurstCapacity(plan.getBurstCapacity());
                config.setRequestedTokens(plan.getRequestedTokens());
                
                limiter.getConfig().put("default", config);
                limiters.put(pkg.toUpperCase(), limiter);
            });
        }
    }

    @Override
    public Mono<Response> isAllowed(String routeId, String id) {
        // Step 1: Look up client tier efficiently safely via Caffeine + Redis cache separation 
        return tierResolver.resolveTier(id)
                .defaultIfEmpty(defaultPackage)
                .flatMap(clientTier -> {
                    RedisRateLimiter limiter = limiters.get(clientTier.toUpperCase());
                    if (limiter == null && !limiters.isEmpty()) {
                        limiter = limiters.values().iterator().next(); // safe fallback
                    }
                    
                    if (limiter == null) return doMetricReturn(new Response(true, new HashMap<>()));

                    // Run the robust Redis check wrapped gracefully by Resilience4j
                    return circuitBreaker.run(
                        limiter.isAllowed("default", id).flatMap(this::doMetricReturn),
                        throwable -> recoverFallback(id, throwable)
                    );
                })
                .onErrorResume(throwable -> recoverFallback(id, throwable));
    }
    
    // Recovery Fallback gracefully routes into Caffeine caching rather than killing upstream traffic
    private Mono<Response> recoverFallback(String id, Throwable throwable) {
        log.warn("Redis Circuit broken or disconnected for identity: {}. Routing to Local Rate Limiter constraints.", id);
        // Using `isLocalAllowed` limits each user down to explicitly 5 burst capacity per 60s when Redis drops entirely. 
        boolean allowedLocal = cacheService.isLocalAllowed(id, 5); 
        Map<String, String> headers = new HashMap<>();
        headers.put("X-RateLimit-Fallback", "true");
        return doMetricReturn(new Response(allowedLocal, headers));
    }

    // Capture standard Metrics automatically without spamming filters
    private Mono<Response> doMetricReturn(Response response) {
        if (response.isAllowed()) {
            allowedRequestsCounter.increment();
        } else {
            blockedRequestsCounter.increment();
        }
        return Mono.just(response);
    }
    
    @Override
    public Map<String, RedisRateLimiter.Config> getConfig() {
        return new HashMap<>();
    }

    @Override
    public Class<RedisRateLimiter.Config> getConfigClass() {
        return RedisRateLimiter.Config.class;
    }

    @Override
    public RedisRateLimiter.Config newConfig() {
        return new RedisRateLimiter.Config();
    }
}
