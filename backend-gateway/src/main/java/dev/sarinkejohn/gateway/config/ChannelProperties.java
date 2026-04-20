package dev.sarinkejohn.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "app.channel")
public class ChannelProperties {

    private List<String> allowedList = List.of("WEBP", "SWIFTY", "BANKAPP", "MOBILEMONEY", "SAMONEY");
    private Map<String, String> tierMap = Map.of(
            "WEBP", "SILVER",
            "SWIFTY", "GOLD",
            "BANKAPP", "GOLD",
            "MOBILEMONEY", "BRONZE",
            "SAMONEY", "BRONZE"
    );

    public List<String> getAllowedList() { return allowedList; }
    public void setAllowedList(List<String> allowedList) { this.allowedList = allowedList; }

    public Map<String, String> getTierMap() { return tierMap; }
    public void setTierMap(Map<String, String> tierMap) { this.tierMap = tierMap; }

    /**
     * Check if a channel is in the allowed channel list (case-insensitive).
     */
    public boolean isAllowed(String channel) {
        if (channel == null) return false;
        return allowedList.stream()
                .anyMatch(c -> c.equalsIgnoreCase(channel));
    }

    /**
     * Get the rate-limit tier for a given channel.
     * Falls back to null if no mapping exists (caller should use default package).
     */
    public String getTierForChannel(String channel) {
        if (channel == null) return null;
        // Try exact match first, then case-insensitive
        String tier = tierMap.get(channel.toUpperCase());
        return tier;
    }
}
