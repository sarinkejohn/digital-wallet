package dev.sarinkejohn.gateway_demo;

import dev.sarinkejohn.gateway.config.ChannelProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.hamcrest.Matchers.containsString;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class JsonSchemaValidationFilterIntegrationTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ChannelProperties channelProperties;

    @Test
    public void whenValidRequest_thenSuccess() {
        webClient.get()
                .uri("/api/test")
                .header("Authorization", "Bearer token123")
                .header("Content-Type", "application/json")
                .header("Channel", "WEBP")
                .header("AppId", "WEBP")
                .header("AppVersion", "1.0.0")
                .header("RequestId", "test-request-valid")
                .exchange()
                .expectStatus().value(v -> {
                    // Success means the filter didn't return a 400.
                    // Since we don't have a backend mock, 500 or 404 are acceptable "passed the filter" states.
                    assert v == 404 || v == 500;
                });
    }

    @Test
    public void whenInvalidChannel_thenReturnError1000003() {
        String invalidChannel = "FORBIDDEN_CHANNEL";
        String requestId = "test-req-id-invalid-channel";

        webClient.get()
                .uri("/api/test")
                .header("Authorization", "Bearer token123")
                .header("Content-Type", "application/json")
                .header("Channel", invalidChannel)
                .header("AppId", "WEBP")
                .header("AppVersion", "1.0.0")
                .header("RequestId", requestId)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("ERR_1000003")
                .jsonPath("$.errorMessage").isEqualTo("Invalid channel")
                .jsonPath("$.errorDescription").value(containsString(invalidChannel))
                .jsonPath("$.requestId").isEqualTo(requestId)
                .jsonPath("$.channel").isEqualTo(invalidChannel);
    }

    @Test
    public void whenMissingMandatoryHeaders_thenReturnError1000001() {
        webClient.get()
                .uri("/api/test")
                .header("Authorization", "Bearer token123")
                // Missing Content-Type, Channel, AppId, etc.
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("ERR_1000001")
                .jsonPath("$.requestId").isEqualTo("N/A")
                .jsonPath("$.channel").isEqualTo("N/A");
    }

    @Test
    public void whenRequestIdTooLong_thenReturnError1000002() {
        String longRequestId = "a".repeat(41); // Exceeds default limit (40 set in .env now)
        String channel = "WEBP";

        webClient.get()
                .uri("/api/test")
                .header("Authorization", "Bearer token123")
                .header("Content-Type", "application/json")
                .header("Channel", channel)
                .header("AppId", "WEBP")
                .header("AppVersion", "1.0.0")
                .header("RequestId", longRequestId)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("ERR_1000002")
                .jsonPath("$.requestId").isEqualTo(longRequestId)
                .jsonPath("$.channel").isEqualTo(channel);
    }
}
