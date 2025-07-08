package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.autoconfiguration.TestApplication;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.Map;
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
class RateLimiterIntegrationTest {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("group.http.clients.rl.base-url", () -> wiremock.getRuntimeInfo()
                .getHttpBaseUrl());
        registry.add("group.http.clients.rl.resilience.rate-limiter-enabled", () -> "true");
        registry.add("group.http.clients.rl.resilience.rate-limiter.limit-for-period", () -> "1");
        registry.add("group.http.clients.rl.resilience.rate-limiter.limit-refresh-period", () -> "10s");
        registry.add("group.http.clients.rl.resilience.rate-limiter.timeout-duration", () -> "0");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @BeforeEach
    void configureStubs() {
        configureFor("localhost", wiremock.getRuntimeInfo().getHttpPort());
        stubFor(get("/ping")
                .willReturn(aResponse().withStatus(HttpStatus.OK.value()).withBody("pong")));
    }

    @Test
    void shouldAllowOnlyOneCallDueToRateLimiting() {
        RestClient client = clients.get("rl");

        String response = client.get().uri("/ping").retrieve().body(String.class);
        assertThat(response).isEqualTo("pong");

        assertThatThrownBy(() -> client.get().uri("/ping").retrieve().body(String.class))
                .hasMessageContaining("does not permit further calls");
    }
}
