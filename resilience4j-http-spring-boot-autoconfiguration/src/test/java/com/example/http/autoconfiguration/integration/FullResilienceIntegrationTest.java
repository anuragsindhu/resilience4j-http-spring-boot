package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.autoconfiguration.TestApplication;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

/**
 * Time ↑
 * │
 * │
 * t1 ──▶ RateLimiter → permitCount = 1 → acquired
 * │  └─→ breakerState = CLOSED
 * │  └─→ Retry attempt #1 → response 502
 * │  └─→ Retry attempt #2 → response 502
 * │  └─→ failureCount = 1 (breaker window: [failure])
 * │  └─→ breakerState remains CLOSED
 * <p>
 * t2 ──▶ RateLimiter → permitCount = 0 → rejected
 * │  └─→ RequestNotPermitted thrown
 * │  └─→ Retry and CircuitBreaker not invoked
 * │  └─→ breaker window unchanged (still [failure])
 * <p>
 * t3 ──▶ RateLimiter → permitCount = 0 → rejected
 * │  └─→ RequestNotPermitted thrown
 * │  └─→ breakerState = CLOSED
 * │  └─→ failureCount = 1
 * <p>
 * t4 ──▶ RateLimiter → permitCount = 0 → rejected
 * │  └─→ RequestNotPermitted thrown
 * │  └─→ breakerState = CLOSED
 * <p>
 * t5 ──▶ CircuitBreaker window never reaches 3 calls
 * │  └─→ breakerState remains CLOSED
 * │  └─→ failureRate = 33% (based on 1/3 threshold, but only 1 actual call made)
 * │
 * ▼
 */
@SpringBootTest(classes = TestApplication.class)
class FullResilienceIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("group.http.clients.resilient.base-url", () -> wm.getRuntimeInfo()
                .getHttpBaseUrl());
        registry.add("group.http.clients.resilient.resilience.circuit-breaker-enabled", () -> "true");
        registry.add("group.http.clients.resilient.resilience.retry-enabled", () -> "true");
        registry.add("group.http.clients.resilient.resilience.rate-limiter-enabled", () -> "true");
        registry.add("group.http.clients.resilient.resilience.retry.max-attempts", () -> "2");
        registry.add("group.http.clients.resilient.resilience.rate-limiter.limit-for-period", () -> "3");
        registry.add("group.http.clients.resilient.resilience.rate-limiter.limit-refresh-period", () -> "10s");
        registry.add("group.http.clients.resilient.resilience.rate-limiter.timeout-duration", () -> "0");
        registry.add("group.http.clients.resilient.resilience.circuit-breaker.minimum-number-of-calls", () -> "3");
        registry.add("group.http.clients.resilient.resilience.circuit-breaker.sliding-window-size", () -> "3");
        registry.add("group.http.clients.resilient.resilience.circuit-breaker.failure-rate-threshold", () -> "50.0");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void stubFailure() {
        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());
        stubFor(get("/combo").willReturn(aResponse().withStatus(502)));
    }

    @Test
    void shouldVerifyRetryThenCircuitBreakerThenRateLimiter() {
        RestClient client = clients.get("resilient");

        // 3 permitted requests, each will trigger retry and failure
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> client.get().uri("/combo").retrieve().body(String.class))
                    .hasMessageContaining("502");
        }

        // Circuit breaker should now reject calls
        assertThat(circuitBreakerRegistry.circuitBreaker("resilient").getState())
                .isEqualTo(io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN);

        assertThatThrownBy(() -> client.get().uri("/combo").retrieve().body(String.class))
                .isInstanceOfAny(CallNotPermittedException.class, RequestNotPermitted.class)
                .hasMessageContaining("does not permit");

        // Rate limiter should reject next call due to permit exhaustion
        assertThatThrownBy(() -> client.get().uri("/combo").retrieve().body(String.class))
                .isInstanceOf(RequestNotPermitted.class)
                .hasMessageContaining("does not permit further calls");
    }
}
