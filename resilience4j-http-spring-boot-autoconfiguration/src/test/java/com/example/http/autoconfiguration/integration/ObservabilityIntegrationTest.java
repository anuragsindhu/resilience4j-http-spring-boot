package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.autoconfiguration.TestApplication;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.micrometer.common.KeyValue;
import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
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
import org.springframework.web.client.RestClient;

@SpringBootTest(classes = TestApplication.class)
@ContextConfiguration(classes = ObservabilityIntegrationTest.ObsTestConfig.class)
class ObservabilityIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @Autowired
    private Map<String, RestClient> clients;

    @Autowired
    private ObservationRegistry registry;

    @Autowired
    private TestObservationHandler obsHandler;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        String base = wm.getRuntimeInfo().getHttpBaseUrl();

        registry.add("group.http.clients.o11y.base-url", () -> base);
        registry.add("group.http.clients.o11y.resilience.retry-enabled", () -> "true");
        registry.add("group.http.clients.o11y.resilience.circuit-breaker-enabled", () -> "false");
        registry.add("group.http.clients.o11y.resilience.rate-limiter-enabled", () -> "false");
        registry.add("group.http.clients.o11y.observation-tags.team", () -> "infra");
        registry.add("group.http.clients.o11y.observation-tags.module", () -> "edge");
    }

    @BeforeEach
    void setup() {
        obsHandler.clear();
        registry.observationConfig().observationHandler(obsHandler);

        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());
        wm.stubFor(get("/ping").willReturn(ok("pong")));
        wm.stubFor(get("/error").willReturn(serverError()));
    }

    @Test
    void shouldRecordObservationOnSuccess() {
        String body = clients.get("o11y").get().uri("/ping").retrieve().body(String.class);
        assertThat(body).isEqualTo("pong");

        Observation.Context ctx = findFirstClientObservation();
        assertThat(ctx).isNotNull();

        assertThat(ctx.getLowCardinalityKeyValues())
                .as("Expected fixed and custom observation tags")
                .anySatisfy(KeyValueMatcher.of("client", "o11y"))
                .anySatisfy(KeyValueMatcher.of("http.method", "GET"))
                .anySatisfy(KeyValueMatcher.of("http.uri", "/ping"))
                .anySatisfy(KeyValueMatcher.of("team", "infra"))
                .anySatisfy(KeyValueMatcher.of("module", "edge"));

        assertHighCardinalityHints(ctx);
    }

    @Test
    void shouldRecordObservationOnError() {
        assertThatThrownBy(
                        () -> clients.get("o11y").get().uri("/error").retrieve().body(String.class))
                .isInstanceOf(Exception.class);

        Observation.Context ctx = findFirstClientObservation();
        assertThat(ctx).isNotNull();

        assertThat(ctx.getLowCardinalityKeyValues())
                .as("Expected fixed and custom observation tags")
                .anySatisfy(KeyValueMatcher.of("client", "o11y"))
                .anySatisfy(KeyValueMatcher.of("http.method", "GET"))
                .anySatisfy(KeyValueMatcher.of("http.uri", "/error"))
                .anySatisfy(KeyValueMatcher.of("team", "infra"))
                .anySatisfy(KeyValueMatcher.of("module", "edge"));

        assertHighCardinalityHints(ctx);
    }

    private Observation.Context findFirstClientObservation() {
        return obsHandler.getCompleted().stream()
                .filter(ctx -> "http.client.request.resilient".equals(ctx.getName()))
                .findFirst()
                .orElse(null);
    }

    private void assertHighCardinalityHints(Observation.Context ctx) {
        if (ctx.getHighCardinalityKeyValues().stream()
                .anyMatch(kv -> kv.getKey().equals("duration.ms"))) {
            KeyValue dur = ctx.getHighCardinalityKeyValues().stream()
                    .filter(kv -> kv.getKey().equals("duration.ms"))
                    .findFirst()
                    .orElseThrow();

            assertThat(dur.getValue()).as("Duration should be a numeric string").matches("\\d+");
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class ObsTestConfig {
        @Bean
        public TestObservationHandler testObservationHandler() {
            return new TestObservationHandler();
        }
    }

    static class TestObservationHandler implements ObservationHandler<Context> {

        private final List<Context> completed = new ArrayList<>();

        public List<Context> getCompleted() {
            return completed;
        }

        public void clear() {
            completed.clear();
        }

        @Override
        public boolean supportsContext(Context context) {
            return true;
        }

        @Override
        public void onStart(Context context) {
            // no-op
        }

        @Override
        public void onError(Context context) {
            // no-op
        }

        @Override
        public void onStop(Context context) {
            completed.add(context);
        }
    }

    static class KeyValueMatcher {
        public static Consumer<KeyValue> of(String key, String value) {
            return kv -> assertThat(kv)
                    .extracting(KeyValue::getKey, KeyValue::getValue)
                    .containsExactly(key, value);
        }
    }
}
