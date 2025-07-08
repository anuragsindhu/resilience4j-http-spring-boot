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
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(classes = TestApplication.class)
class CircuitBreakerIntegrationTest {
    @Autowired
    private CircuitBreakerRegistry registry;

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("group.http.clients.cb.base-url", () -> wiremock.getRuntimeInfo()
                .getHttpBaseUrl());
        registry.add("group.http.clients.cb.resilience.circuit-breaker-enabled", () -> "true");
        registry.add("group.http.clients.cb.resilience.circuit-breaker.sliding-window-size", () -> "3");
        registry.add("group.http.clients.cb.resilience.circuit-breaker.minimum-number-of-calls", () -> "3");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @BeforeEach
    void configureStubs() {
        configureFor("localhost", wiremock.getRuntimeInfo().getHttpPort());
        stubFor(get("/unstable").willReturn(aResponse().withStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

    @Test
    @SneakyThrows
    void circuitShouldOpenAfterFailureThresholdIsMet() {
        RestClient client = clients.get("cb");

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> client.get().uri("/unstable").retrieve().body(String.class))
                    .hasMessageContaining("500");
        }

        assertThatThrownBy(() -> client.get().uri("/unstable").retrieve().body(String.class))
                .isInstanceOf(CallNotPermittedException.class);
    }
}
