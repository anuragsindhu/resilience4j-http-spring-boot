package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
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
class RetryIntegrationTest {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("group.http.clients.retry.base-url", () -> wiremock.getRuntimeInfo()
                .getHttpBaseUrl());
        registry.add("group.http.clients.retry.resilience.retry-enabled", () -> "true");
        registry.add("group.http.clients.retry.resilience.retry.max-attempts", () -> "2");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @BeforeEach
    void configureStubs() {
        configureFor("localhost", wiremock.getRuntimeInfo().getHttpPort());
        stubFor(get("/retry-me").willReturn(aResponse().withStatus(HttpStatus.BAD_GATEWAY.value())));
    }

    @Test
    void shouldRetryUntilMaxAttemptsAndThenThrow() {
        RestClient client = clients.get("retry");

        assertThatThrownBy(() -> client.get().uri("/retry-me").retrieve().body(String.class))
                .hasMessageContaining("502");
    }
}
