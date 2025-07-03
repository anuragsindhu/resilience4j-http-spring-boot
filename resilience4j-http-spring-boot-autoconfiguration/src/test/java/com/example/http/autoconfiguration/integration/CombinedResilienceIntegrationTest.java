package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.*;

import com.example.http.autoconfiguration.TestApplication;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@SpringBootTest(classes = TestApplication.class)
class CombinedResilienceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry reg) {
        String baseUrl = wm.getRuntimeInfo().getHttpBaseUrl();
        reg.add("group.http.clients.combined-resil.base-url", () -> baseUrl);

        // RateLimiter: high limit so it never blocks here
        reg.add("group.http.clients.combined-resil.resilience.rate-limiter-enabled", () -> "true");
        reg.add("group.http.clients.combined-resil.resilience.rate-limiter.limit-for-period", () -> "100");
        reg.add("group.http.clients.combined-resil.resilience.rate-limiter.limit-refresh-period", () -> "PT1S");
        reg.add("group.http.clients.combined-resil.resilience.rate-limiter.timeout-duration", () -> "PT0S");

        // CircuitBreaker: opens at ≥50% failures over 2 calls
        reg.add("group.http.clients.combined-resil.resilience.circuit-breaker-enabled", () -> "true");
        reg.add("group.http.clients.combined-resil.resilience.circuit-breaker.failure-rate-threshold", () -> "50.0");
        reg.add("group.http.clients.combined-resil.resilience.circuit-breaker.sliding-window-size", () -> "2");

        // Retry: 2 attempts, zero wait
        reg.add("group.http.clients.combined-resil.resilience.retry-enabled", () -> "true");
        reg.add("group.http.clients.combined-resil.resilience.retry.max-attempts", () -> "3");
        reg.add("group.http.clients.combined-resil.resilience.retry.wait-duration", () -> "PT0S");
        reg.add("group.http.clients.combined-resil.resilience.retry.retry-status[0]", () -> "INTERNAL_SERVER_ERROR");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @BeforeEach
    void setupStubs() {
        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());

        // /first: two 500s then 200 OK
        stubFor(get("/first")
                .inScenario("firstScenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("firstFailedOnce"));

        stubFor(get("/first")
                .inScenario("firstScenario")
                .whenScenarioStateIs("firstFailedOnce")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("firstSuccess"));

        stubFor(get("/first")
                .inScenario("firstScenario")
                .whenScenarioStateIs("firstSuccess")
                .willReturn(aResponse().withStatus(200).withBody("OK").withHeader("Content-Type", "text/plain")));

        // /second: always 500
        stubFor(get("/second").willReturn(aResponse().withStatus(500)));
    }

    @Test
    void shouldRetryThenOpenCircuitWhenAllFeaturesEnabled() {
        RestClient client = clients.get("combined-resil");

        // 1) /first: Retry=2 → two 500s, then OK
        String body = client.get().uri("/first").retrieve().body(String.class);
        assertThat(body).isEqualTo("OK");

        // 2) /second: Retry=2 → two 500s → HttpServerErrorException
        assertThatThrownBy(() -> client.get().uri("/second").retrieve().body(String.class))
                .isInstanceOf(HttpServerErrorException.class)
                .hasMessageContaining("500");

        // 3) /second again: CircuitBreaker now OPEN → CallNotPermittedException
        assertThatThrownBy(() -> client.get().uri("/second").retrieve().body(String.class))
                .isInstanceOf(CallNotPermittedException.class);
    }
}
