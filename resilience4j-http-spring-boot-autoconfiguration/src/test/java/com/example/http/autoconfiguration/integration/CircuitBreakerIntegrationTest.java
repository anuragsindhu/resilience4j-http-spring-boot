package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.springframework.web.client.RestClient;

@SpringBootTest(classes = TestApplication.class)
class CircuitBreakerIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("group.http.clients.cb.base-url", () -> wm.getRuntimeInfo().getHttpBaseUrl());
        registry.add("group.http.clients.cb.resilience.circuit-breaker-enabled", () -> "true");
        registry.add("group.http.clients.cb.resilience.circuit-breaker.failure-rate-threshold", () -> "50.0");
        registry.add("group.http.clients.cb.resilience.circuit-breaker.sliding-window-size", () -> "2");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @BeforeEach
    void stubAlways500() {
        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());

        // every call to /unstable returns HTTP 500
        stubFor(get("/unstable").willReturn(aResponse().withStatus(500)));
    }

    @Test
    void circuitOpensAfterFailures() {
        RestClient client = clients.get("cb");

        // first two calls fail, counting toward circuit threshold
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> client.get().uri("/unstable").retrieve().body(String.class))
                    .hasMessageContaining("500");
        }

        // third call should be short-circuited
        assertThatThrownBy(() -> client.get().uri("/unstable").retrieve().body(String.class))
                .isInstanceOf(CallNotPermittedException.class);
    }
}
