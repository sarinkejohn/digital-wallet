package dev.sarinkejohn.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Configuration
public class RateLimiterConfig {

    @Value("${APP_RATE_LIMIT_PACKAGE:SILVER}")
    private String defaultPackage;

    @Bean
    public KeyResolver clientKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName)
                .switchIfEmpty(Mono.defer(() -> {
                    String fallback = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst("AppId"))
                            .filter(id -> !id.isEmpty())
                            .orElseGet(() -> {
                                if (exchange.getRequest().getRemoteAddress() != null && exchange.getRequest().getRemoteAddress().getAddress() != null) {
                                    return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
                                }
                                return "anonymous";
                            });
                    return Mono.just(fallback);
                }));
    }

    @Bean
    @Primary
    public TieredDynamicRateLimiter tieredRateLimiter(
            ReactiveStringRedisTemplate redisTemplate,
            RedisScript<List<Long>> redisScript,
            ConfigurationService configurationService,
            RateLimitProperties properties,
            dev.sarinkejohn.gateway.limiting.TierResolver tierResolver,
            dev.sarinkejohn.gateway.limiting.CacheService cacheService,
            org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory circuitBreakerFactory,
            io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        
        return new TieredDynamicRateLimiter(redisTemplate, redisScript, configurationService, properties, 
                defaultPackage, tierResolver, cacheService, circuitBreakerFactory, meterRegistry);
    }
}
