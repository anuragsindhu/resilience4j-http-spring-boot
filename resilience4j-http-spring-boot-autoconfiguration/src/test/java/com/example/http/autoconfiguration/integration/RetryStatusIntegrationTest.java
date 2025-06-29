package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.*;

import com.example.http.autoconfiguration.TestApplication;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
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
class RetryStatusIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        String base = wm.getRuntimeInfo().getHttpBaseUrl();
        registry.add("group.http.clients.retry-status.base-url", () -> base);

        // enable retry only
        registry.add("group.http.clients.retry-status.resilience.retry-enabled", () -> "true");
        registry.add(
                "group.http.clients.retry-status.resilience.retry.retry-status",
                () -> "TOO_MANY_REQUESTS,BAD_GATEWAY,SERVICE_UNAVAILABLE");
        registry.add("group.http.clients.retry-status.resilience.retry.max-attempts", () -> "4");
        registry.add("group.http.clients.retry-status.resilience.retry.wait-duration", () -> "PT0S");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @BeforeEach
    void setupStubs() {
        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());

        // /retry-success: 429 → 502 → 503 → 200
        stubFor(get("/retry-success")
                .inScenario("retryStatus")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(429))
                .willSetStateTo("first429"));

        stubFor(get("/retry-success")
                .inScenario("retryStatus")
                .whenScenarioStateIs("first429")
                .willReturn(aResponse().withStatus(502))
                .willSetStateTo("second502"));

        stubFor(get("/retry-success")
                .inScenario("retryStatus")
                .whenScenarioStateIs("second502")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("third503"));

        stubFor(get("/retry-success")
                .inScenario("retryStatus")
                .whenScenarioStateIs("third503")
                .willReturn(aResponse().withStatus(200).withBody("OK").withHeader("Content-Type", "text/plain")));

        // /retry-no-retry: always 500 (not in retry-status)
        stubFor(get("/retry-no-retry").willReturn(aResponse().withStatus(500)));
    }

    @Test
    void shouldRetryOnConfiguredStatusesAndSucceed() {
        RestClient client = clients.get("retry-status");

        String result = client.get().uri("/retry-success").retrieve().body(String.class);

        assertThat(result).isEqualTo("OK");
        // verify all 4 attempts were made
        verify(4, getRequestedFor(urlEqualTo("/retry-success")));
    }

    @Test
    void shouldNotRetryOnUnlistedStatusAndFailFast() {
        RestClient client = clients.get("retry-status");

        assertThatThrownBy(() -> client.get().uri("/retry-no-retry").retrieve().body(String.class))
                .isInstanceOf(HttpServerErrorException.class)
                .hasMessageContaining("500");

        // only one call, no retries
        verify(1, getRequestedFor(urlEqualTo("/retry-no-retry")));
    }
}
