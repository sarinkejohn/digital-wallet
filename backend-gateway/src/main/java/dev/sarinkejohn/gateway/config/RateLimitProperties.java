package dev.sarinkejohn.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private String defaultPackage = "SILVER";
    private Map<String, Plan> plans;

    public String getDefaultPackage() { return defaultPackage; }
    public void setDefaultPackage(String defaultPackage) { this.defaultPackage = defaultPackage; }

    public Map<String, Plan> getPlans() { return plans; }
    public void setPlans(Map<String, Plan> plans) { this.plans = plans; }

    public static class Plan {
        private int replenishRate = 1;
        private int burstCapacity = 1;
        private int requestedTokens = 1;

        public int getReplenishRate() { return replenishRate; }
        public void setReplenishRate(int replenishRate) { this.replenishRate = replenishRate; }
        
        public int getBurstCapacity() { return burstCapacity; }
        public void setBurstCapacity(int burstCapacity) { this.burstCapacity = burstCapacity; }

        public int getRequestedTokens() { return requestedTokens; }
        public void setRequestedTokens(int requestedTokens) { this.requestedTokens = requestedTokens; }
    }
}
