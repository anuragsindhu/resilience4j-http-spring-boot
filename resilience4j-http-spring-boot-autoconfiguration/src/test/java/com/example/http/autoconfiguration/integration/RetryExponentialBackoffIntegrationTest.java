package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.autoconfiguration.TestApplication;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
class RetryExponentialBackoffIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private Map<String, RestClient> clients;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry reg) {
        String base = wm.getRuntimeInfo().getHttpBaseUrl();

        // Common retry status for all clients
        String retryStatusKey = "resilience.retry.retry-status[0]";
        String retryStatusValue = "INTERNAL_SERVER_ERROR";

        // 1. Exponential + jitter + max wait
        reg.add("group.http.clients.exp-jitter-max.base-url", () -> base);
        reg.add("group.http.clients.exp-jitter-max.resilience.retry-enabled", () -> "true");
        reg.add("group.http.clients.exp-jitter-max.resilience.retry.max-attempts", () -> "4");
        reg.add("group.http.clients.exp-jitter-max.resilience.retry.wait-duration", () -> "100ms");
        reg.add("group.http.clients.exp-jitter-max.resilience.retry.exponential-backoff-multiplier", () -> "2.0");
        reg.add("group.http.clients.exp-jitter-max.resilience.retry.randomized-wait-factor", () -> "0.25");
        reg.add("group.http.clients.exp-jitter-max." + retryStatusKey, () -> retryStatusValue);

        // 2. Exponential + jitter (no max wait)
        reg.add("group.http.clients.exp-jitter.base-url", () -> base);
        reg.add("group.http.clients.exp-jitter.resilience.retry-enabled", () -> "true");
        reg.add("group.http.clients.exp-jitter.resilience.retry.max-attempts", () -> "4");
        reg.add("group.http.clients.exp-jitter.resilience.retry.wait-duration", () -> "100ms");
        reg.add("group.http.clients.exp-jitter.resilience.retry.exponential-backoff-multiplier", () -> "2.0");
        reg.add("group.http.clients.exp-jitter.resilience.retry.randomized-wait-factor", () -> "0.5");
        reg.add("group.http.clients.exp-jitter." + retryStatusKey, () -> retryStatusValue);

        // 3. Jittered fixed interval only
        reg.add("group.http.clients.jitter-only.base-url", () -> base);
        reg.add("group.http.clients.jitter-only.resilience.retry-enabled", () -> "true");
        reg.add("group.http.clients.jitter-only.resilience.retry.max-attempts", () -> "4");
        reg.add("group.http.clients.jitter-only.resilience.retry.wait-duration", () -> "300ms");
        reg.add("group.http.clients.jitter-only.resilience.retry.randomized-wait-factor", () -> "0.3");
        reg.add("group.http.clients.jitter-only." + retryStatusKey, () -> retryStatusValue);
    }

    @BeforeEach
    void stubAlways500() {
        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());
        stubFor(get("/retry-target").willReturn(serverError()));
    }

    @Test
    void exponentialBackoffWithJitterAndMaxWait() {
        RestClient client = clients.get("exp-jitter-max");
        long elapsed = callAndMeasure(
                () -> client.get().uri("/retry-target").retrieve().body(String.class));
        assertThat(elapsed).isBetween(300L, 900L);
    }

    @Test
    void exponentialBackoffWithJitterOnly() {
        RestClient client = clients.get("exp-jitter");
        long elapsed = callAndMeasure(
                () -> client.get().uri("/retry-target").retrieve().body(String.class));
        assertThat(elapsed).isBetween(300L, 800L);
    }

    @Test
    void fixedIntervalWithJitterOnly() {
        RestClient client = clients.get("jitter-only");
        long elapsed = callAndMeasure(
                () -> client.get().uri("/retry-target").retrieve().body(String.class));
        assertThat(elapsed).isBetween(600L, 1100L);
    }

    private long callAndMeasure(ExecutableRequest call) {
        long start = System.nanoTime();
        assertThatThrownBy(call::invoke).isInstanceOf(HttpServerErrorException.class);
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
    }

    @FunctionalInterface
    interface ExecutableRequest {
        void invoke();
    }
}
