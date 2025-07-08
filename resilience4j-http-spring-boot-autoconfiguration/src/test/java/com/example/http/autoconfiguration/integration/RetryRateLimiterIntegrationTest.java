package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.autoconfiguration.TestApplication;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(classes = TestApplication.class)
class RetryRateLimiterIntegrationTest {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("group.http.clients.rlretry.base-url", () -> wiremock.getRuntimeInfo()
                .getHttpBaseUrl());
        registry.add("group.http.clients.rlretry.resilience.retry-enabled", () -> "true");
        registry.add("group.http.clients.rlretry.resilience.rate-limiter-enabled", () -> "true");
        registry.add("group.http.clients.rlretry.resilience.retry.max-attempts", () -> "2");
        registry.add("group.http.clients.rlretry.resilience.rate-limiter.limit-for-period", () -> "1");
        registry.add("group.http.clients.rlretry.resilience.rate-limiter.limit-refresh-period", () -> "10s");
        registry.add("group.http.clients.rlretry.resilience.rate-limiter.timeout-duration", () -> "0");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @BeforeEach
    void stubFailure() {
        configureFor("localhost", wiremock.getRuntimeInfo().getHttpPort());
        stubFor(get("/retry-limit").willReturn(aResponse().withStatus(502)));
    }

    @Test
    void shouldRetryAndThenTriggerRateLimit() {
        RestClient client = clients.get("rlretry");

        assertThatThrownBy(() -> client.get().uri("/retry-limit").retrieve().body(String.class))
                .hasMessageContaining("502");

        assertThatThrownBy(() -> client.get().uri("/retry-limit").retrieve().body(String.class))
                .hasMessageContaining("does not permit further calls");
    }
}
