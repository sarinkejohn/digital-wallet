package dev.sarinkejohn.gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import dev.sarinkejohn.gateway.config.ChannelProperties;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JsonSchemaValidationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaValidationFilter.class);

    private final ObjectMapper mapper = new ObjectMapper();
    private final ChannelProperties channelProperties;
    private JsonSchema schema = null;

    @Value("${REQUEST_ID_MAX_LENGTH:36}")
    private int requestIdMaxLength;

    public JsonSchemaValidationFilter(ChannelProperties channelProperties) {
        this.channelProperties = channelProperties;
    }

    @PostConstruct
    public void initSchema() {
        try {
            ClassPathResource r = new ClassPathResource("schema/gateway-headers-schema.json");
            JsonNode schemaNode = mapper.readTree(r.getInputStream());
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            this.schema = factory.getSchema(schemaNode.get("definitions").get("headers"));
            log.info("Loaded JSON schema for headers validation");
        } catch (Exception e) {
            log.error("Failed to load JSON schema (filter will be disabled): {}", e.getMessage(), e);
            this.schema = null;
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (this.schema == null) {
            log.trace("Schema not loaded, skipping header validation");
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        
        // Skip validation for CORS preflight OPTIONS requests
        if (request.getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }
        Map<String, String> headers = new HashMap<>();

        // normalize headers into case-sensitive keys as per schema
        putIfPresent(headers, "Authorization", request.getHeaders().getFirst("Authorization"));
        putIfPresent(headers, "Content-Type", request.getHeaders().getFirst("Content-Type"));
        putIfPresent(headers, "Channel", request.getHeaders().getFirst("Channel"));
        putIfPresent(headers, "AppId", request.getHeaders().getFirst("AppId"));
        putIfPresent(headers, "AppVersion", request.getHeaders().getFirst("AppVersion"));
        putIfPresent(headers, "RequestId", request.getHeaders().getFirst("RequestId"));

        // Extract requestId and channel for primary validation and error reporting
        String requestId = headers.get("RequestId");
        String channel = headers.get("Channel");

        // 1. Check RequestId length specifically (ERR_1000002)
        if (requestId != null && requestId.length() > requestIdMaxLength) {
            log.debug("RequestId length {} exceeds maximum {}", requestId.length(), requestIdMaxLength);
            return writeError(exchange.getResponse(),
                    "ERR_1000002",
                    "Invalid parameter length",
                    "RequestId exceeds maximum allowed length of " + requestIdMaxLength,
                    requestId,
                    channel);
        }

        // 2. Check Channel allow-list specifically (ERR_1000003)
        if (channel != null && !channelProperties.isAllowed(channel)) {
            log.debug("Channel '{}' is not in the allowed channel list: {}", channel, channelProperties.getAllowedList());
            return writeError(exchange.getResponse(),
                    "ERR_1000003",
                    "Invalid channel",
                    "Channel '" + channel + "' is not in the allowed channel list",
                    requestId,
                    channel);
        }

        // 3. Run full JSON schema validation (for missing headers, enum mismatches, etc.)
        JsonNode headerNode = mapper.valueToTree(headers);
        Set<ValidationMessage> errors = schema.validate(headerNode);

        if (!errors.isEmpty()) {
            List<String> details = errors.stream()
                    .map(ValidationMessage::getMessage)
                    .collect(Collectors.toList());

            log.debug("Header schema validation failed: {}. Node: {}", details, headerNode);

            return writeError(exchange.getResponse(),
                    "ERR_1000001",
                    "Missing mandatory parameters",
                    "One or few mandatory parameters are missing in the request",
                    requestId,
                    channel);
        }

        // Resolve the channel's rate-limit tier and pass it downstream via request header
        String channelTier = channelProperties.getTierForChannel(channel);
        if (channelTier != null) {
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-Channel-Tier", channelTier)
                    .build();
            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        }

        return chain.filter(exchange);
    }

    private Mono<Void> writeError(ServerHttpResponse response,
                                  String errorCode,
                                  String errorMessage,
                                  String errorDescription,
                                  String requestId,
                                  String channel) {
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        response.getHeaders().set("Content-Type", "application/json");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("errorCode", errorCode);
        body.put("errorMessage", errorMessage);
        body.put("errorDescription", errorDescription);
        body.put("requestId", requestId != null ? requestId : "N/A");
        body.put("channel", channel != null ? channel : "N/A");

        byte[] bytes;
        try {
            bytes = mapper.writeValueAsBytes(body);
        } catch (Exception e) {
            bytes = ("{\"errorCode\":\"" + errorCode +
                    "\",\"errorMessage\":\"" + errorMessage +
                    "\",\"errorDescription\":\"" + errorDescription +
                    "\",\"requestId\":\"" + (requestId != null ? requestId : "N/A") +
                    "\",\"channel\":\"" + (channel != null ? channel : "N/A") + "\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }

        DataBufferFactory bufferFactory = response.bufferFactory();
        DataBuffer buffer = bufferFactory.wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    @Override
    public int getOrder() {
        return -100;
    }
}