package dev.sarinkejohn.gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class ChannelValidationFilter extends AbstractGatewayFilterFactory<ChannelValidationFilter.Config> implements Ordered {

    @Value("${jwt.secret:mySecretKeyForDigitalWalletServiceThat'sLongEnoughForHS256Algorithm}")
    private String jwtSecret;

    public ChannelValidationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            ServerHttpResponse response = exchange.getResponse();

            // Skip authentication for public endpoints (auth routes)
            String path = request.getPath().value();
            if (path.startsWith("/api/auth/")) {
                return chain.filter(exchange);
            }

            String requestChannel = request.getHeaders().getFirst("Channel");
            String authHeader = request.getHeaders().getFirst("Authorization");

            if (requestChannel == null || authHeader == null) {
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return response.setComplete();
            }

            if (!authHeader.startsWith("Bearer ")) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            String token = authHeader.substring(7);

            try {
                String tokenChannel = getChannelFromToken(token);

                if (tokenChannel == null) {
                    tokenChannel = "WEBP";
                }

                if (!requestChannel.equalsIgnoreCase(tokenChannel)) {
                    response.setStatusCode(HttpStatus.FORBIDDEN);
                    response.getHeaders().add("X-Error", "Channel mismatch: token issued for " + tokenChannel + " but request from " + requestChannel);
                    return response.setComplete();
                }

            } catch (Exception e) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            return chain.filter(exchange);
        };
    }

    private String getChannelFromToken(String token) {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return claims.get("channel", String.class);
    }

    @Override
    public int getOrder() {
        return -2;
    }

    public static class Config {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}