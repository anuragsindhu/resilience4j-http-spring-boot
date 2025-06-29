package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

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
import java.util.stream.Collectors;
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

        // single client, no resilience features turned on
        registry.add("group.http.clients.o11y.base-url", () -> base);
        registry.add("group.http.clients.o11y.resilience.retry-enabled", () -> "false");
        registry.add("group.http.clients.o11y.resilience.circuit-breaker-enabled", () -> "false");
        registry.add("group.http.clients.o11y.resilience.rate-limiter-enabled", () -> "false");
    }

    @BeforeEach
    void setup() {
        obsHandler.clear();

        registry.observationConfig().observationHandler(obsHandler);

        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());
        stubFor(get("/ping").willReturn(ok("pong")));
        stubFor(get("/error").willReturn(serverError()));
    }

    @Test
    void shouldRecordObservationOnSuccess() {
        String body = clients.get("o11y").get().uri("/ping").retrieve().body(String.class);
        assertThat(body).isEqualTo("pong");

        List<Observation.Context> all = obsHandler.getCompleted();
        assertThat(all).as("some Observation recorded").isNotEmpty();

        List<Observation.Context> ours = all.stream()
                .filter(ctx -> "http.client.request.resilient".equals(ctx.getName()))
                .collect(Collectors.toList());
        assertThat(ours).as("at least one of our custom Observations").isNotEmpty();

        Observation.Context ctx = ours.get(0);
        List<KeyValue> tags = new ArrayList<>();
        ctx.getLowCardinalityKeyValues().forEach(tags::add);

        assertThat(tags)
                .extracting(KeyValue::getKey, KeyValue::getValue)
                .contains(tuple("client", "o11y"), tuple("http.method", "GET"), tuple("http.uri", "/ping"));
    }

    @Test
    void shouldRecordObservationOnError() {
        assertThatThrownBy(
                        () -> clients.get("o11y").get().uri("/error").retrieve().body(String.class))
                .isInstanceOfAny(Exception.class);

        List<Observation.Context> all = obsHandler.getCompleted();
        assertThat(all).as("some Observation recorded").isNotEmpty();

        List<Observation.Context> ours = all.stream()
                .filter(ctx -> "http.client.request.resilient".equals(ctx.getName()))
                .collect(Collectors.toList());
        assertThat(ours).as("at least one of our custom Observations").isNotEmpty();

        Observation.Context ctx = ours.get(0);
        List<KeyValue> tags = new ArrayList<>();
        ctx.getLowCardinalityKeyValues().forEach(tags::add);

        assertThat(tags)
                .extracting(KeyValue::getKey, KeyValue::getValue)
                .contains(tuple("client", "o11y"), tuple("http.method", "GET"), tuple("http.uri", "/error"));
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
}
