package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.autoconfiguration.TestApplication;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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

@SpringBootTest(classes = TestApplication.class)
class RateLimiterIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("group.http.clients.rl.base-url", () -> wm.getRuntimeInfo().getHttpBaseUrl());
        registry.add("group.http.clients.rl.resilience.rate-limiter-enabled", () -> "true");
        registry.add("group.http.clients.rl.resilience.rate-limiter.limit-for-period", () -> "2");
        registry.add("group.http.clients.rl.resilience.rate-limiter.limit-refresh-period", () -> "5s");
        registry.add("group.http.clients.rl.resilience.rate-limiter.timeout-duration", () -> "PT0.5S");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @BeforeEach
    void stubAlways200() {
        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());

        stubFor(get("/ping").willReturn(ok("pong")));
    }

    @Test
    void rateLimiterBlocksOnExcessCalls() {
        RestClient client = clients.get("rl");

        // first two calls permitted
        client.get().uri("/ping").retrieve().body(String.class);
        client.get().uri("/ping").retrieve().body(String.class);

        // third call should be blocked by RateLimiter
        assertThatThrownBy(() -> client.get().uri("/ping").retrieve().body(String.class))
                .isInstanceOf(RequestNotPermitted.class);
    }
}
