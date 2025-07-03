package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;

import com.example.http.autoconfiguration.TestApplication;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@SpringBootTest(classes = TestApplication.class)
@ContextConfiguration(classes = CombinedResilienceObservabilityIntegrationTest.ObsTestConfig.class)
class CombinedResilienceObservabilityIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private CircuitBreakerRegistry cbRegistry;

    @Autowired
    private RetryRegistry retryRegistry;

    @Autowired
    private RateLimiterRegistry rlRegistry;

    @Autowired
    private Map<String, RestClient> clients;

    @Autowired
    private ObservationRegistry registry;

    @Autowired
    private TestObservationHandler obsHandler;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry reg) {
        String base = wm.getRuntimeInfo().getHttpBaseUrl();
        reg.add("group.http.clients.combined-o11y.base-url", () -> base);
        // retry
        reg.add("group.http.clients.combined-o11y.resilience.retry-enabled", () -> "true");
        reg.add("group.http.clients.combined-o11y.resilience.retry.max-attempts", () -> "3");
        reg.add("group.http.clients.combined-o11y.resilience.retry.wait-duration", () -> "PT0S");
        reg.add("group.http.clients.combined-o11y.resilience.retry.retry-status[0]", () -> "INTERNAL_SERVER_ERROR");

        // circuit-breaker
        reg.add("group.http.clients.combined-o11y.resilience.circuit-breaker-enabled", () -> "true");
        reg.add("group.http.clients.combined-o11y.resilience.circuit-breaker.sliding-window-size", () -> "2");
        reg.add("group.http.clients.combined-o11y.resilience.circuit-breaker.failure-rate-threshold", () -> "50");

        // rate-limiter – bump up to 4 permits
        reg.add("group.http.clients.combined-o11y.resilience.rate-limiter-enabled", () -> "true");
        reg.add("group.http.clients.combined-o11y.resilience.rate-limiter.limit-for-period", () -> "10");
        reg.add("group.http.clients.combined-o11y.resilience.rate-limiter.limit-refresh-period", () -> "PT1S");
        reg.add("group.http.clients.combined-o11y.resilience.rate-limiter.timeout-duration", () -> "PT0S");
    }

    @BeforeEach
    void setup() {
        // Reset CB to avoid state sharing among tests
        final String CLIENT_NAME = "combined-o11y";
        cbRegistry.find(CLIENT_NAME).ifPresent(CircuitBreaker::reset);

        obsHandler.clear();
        // register our handler into the existing ObservationRegistry
        registry.observationConfig().observationHandler(obsHandler);

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
    void shouldRecordObservationOnRetrySuccess() {
        RestClient client = clients.get("combined-o11y");

        // 2×500 then OK
        String result = client.get().uri("/first").retrieve().body(String.class);
        assertThat(result).isEqualTo("OK");

        // collect our observations
        List<Observation.Context> all = obsHandler.getCompleted();
        // at least one observation for our resilient call
        List<Observation.Context> ours = new ArrayList<>();
        all.stream()
                .filter(ctx -> "http.client.request.resilient".equals(ctx.getName()))
                .forEach(ours::add);

        assertThat(ours).as("at least one custom Observation for /first").isNotEmpty();

        // inspect the first one
        Observation.Context ctx = ours.get(0);
        List<KeyValue> tags = new ArrayList<>();
        ctx.getLowCardinalityKeyValues().forEach(tags::add);

        assertThat(tags)
                .extracting(KeyValue::getKey, KeyValue::getValue)
                .contains(tuple("client", "combined-o11y"), tuple("http.method", "GET"), tuple("http.uri", "/first"));
    }

    @Test
    @SneakyThrows
    void shouldRecordObservationsOnRetryFailureThenCircuitOpen() {
        RestClient client = clients.get("combined-o11y");

        // 1st call – retry failure
        assertThatThrownBy(() -> client.get().uri("/second").retrieve().body(String.class))
                .isInstanceOf(HttpServerErrorException.class);

        // 2nd call – retry failure (needed to cross sliding window threshold)
        assertThatThrownBy(() -> client.get().uri("/second").retrieve().body(String.class))
                .isInstanceOf(HttpServerErrorException.class);

        // Small delay to allow CB to process state transition
        Thread.sleep(50);

        // 3rd call – now circuit should be open
        Throwable thrown =
                catchThrowable(() -> client.get().uri("/second").retrieve().body(String.class));

        assertThat(thrown)
                .satisfiesAnyOf(
                        t -> assertThat(t).isInstanceOf(CallNotPermittedException.class),
                        t -> assertThat(t).hasCauseInstanceOf(CallNotPermittedException.class),
                        t -> assertThat(t).hasRootCauseInstanceOf(CallNotPermittedException.class));

        // validate observations
        List<Observation.Context> all = obsHandler.getCompleted();
        List<Observation.Context> ours = all.stream()
                .filter(ctx -> "http.client.request.resilient".equals(ctx.getName()))
                .toList();

        assertThat(ours).as("at least two Observations for /second").hasSizeGreaterThanOrEqualTo(2);

        ours.forEach(ctx -> {
            List<KeyValue> tags = new ArrayList<>();
            ctx.getLowCardinalityKeyValues().forEach(tags::add);

            assertThat(tags)
                    .extracting(KeyValue::getKey, KeyValue::getValue)
                    .contains(
                            tuple("client", "combined-o11y"),
                            tuple("http.method", "GET"),
                            tuple("http.uri", "/second"));
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class ObsTestConfig {
        @Bean
        public TestObservationHandler testObservationHandler() {
            return new TestObservationHandler();
        }
    }

    static class TestObservationHandler implements ObservationHandler<Observation.Context> {
        private final List<Observation.Context> completed = new ArrayList<>();

        public List<Observation.Context> getCompleted() {
            return completed;
        }

        public void clear() {
            completed.clear();
        }

        @Override
        public boolean supportsContext(Observation.Context context) {
            return true;
        }

        @Override
        public void onStart(Observation.Context context) {}

        @Override
        public void onError(Observation.Context context) {}

        @Override
        public void onStop(Observation.Context context) {
            completed.add(context);
        }
    }
}
