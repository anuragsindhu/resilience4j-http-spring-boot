package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
class CircuitBreakerRetryIntegrationTest {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("group.http.clients.cbr.base-url", () -> wiremock.getRuntimeInfo()
                .getHttpBaseUrl());
        registry.add("group.http.clients.cbr.resilience.circuit-breaker-enabled", () -> "true");
        registry.add("group.http.clients.cbr.resilience.retry-enabled", () -> "true");
        registry.add("group.http.clients.cbr.resilience.retry.max-attempts", () -> "2");
        registry.add("group.http.clients.cbr.resilience.circuit-breaker.minimum-number-of-calls", () -> "3");
        registry.add("group.http.clients.cbr.resilience.circuit-breaker.sliding-window-size", () -> "3");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @BeforeEach
    void stubFailure() {
        configureFor("localhost", wiremock.getRuntimeInfo().getHttpPort());
        stubFor(get("/fail").willReturn(aResponse().withStatus(500)));
    }

    @Test
    void shouldRetryBeforeBreakerOpens() {
        RestClient client = clients.get("cbr");

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> client.get().uri("/fail").retrieve().body(String.class))
                    .hasMessageContaining("500");
        }

        assertThatThrownBy(() -> client.get().uri("/fail").retrieve().body(String.class))
                .isInstanceOf(CallNotPermittedException.class);
    }
}
