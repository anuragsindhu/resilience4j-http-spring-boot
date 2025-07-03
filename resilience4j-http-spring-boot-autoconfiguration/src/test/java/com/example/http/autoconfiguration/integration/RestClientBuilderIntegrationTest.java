package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.http.autoconfiguration.builder.RestClientBuilder;
import com.example.http.autoconfiguration.properties.HttpClientProperties;
import com.example.http.autoconfiguration.properties.RestClientProperties;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerProperties;
import io.github.resilience4j.springboot3.ratelimiter.autoconfigure.RateLimiterProperties;
import io.micrometer.observation.ObservationRegistry;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RestClientBuilderIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    private RestClientBuilder builder;
    private String baseUrl;

    @BeforeEach
    void setup() {
        wm.resetAll();

        ObservationRegistry obs = ObservationRegistry.create();
        CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
        RetryRegistry retryRegistry = RetryRegistry.ofDefaults();
        RateLimiterRegistry rlRegistry = RateLimiterRegistry.ofDefaults();

        builder = RestClientBuilder.builder()
                .observationRegistry(obs)
                .circuitBreakerRegistry(cbRegistry)
                .retryRegistry(retryRegistry)
                .rateLimiterRegistry(rlRegistry)
                .build();

        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());
        baseUrl = "http://localhost:" + wm.getRuntimeInfo().getHttpPort();
    }

    @Test
    void defaultNoRetryShouldFailFastOn500() {
        stubFor(get("/no-retry").willReturn(aResponse().withStatus(500)));

        RestClientProperties props =
                RestClientProperties.builder().baseUrl(baseUrl).build();
        RestClient client = builder.client("noRetry", props).build();

        assertThrows(
                RestClientResponseException.class,
                () -> client.get().uri("/no-retry").retrieve().body(String.class));
    }

    @Test
    void retryEnabledShouldRetryAndSucceed() {
        stubFor(get("/retry")
                .inScenario("retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("second"));
        stubFor(get("/retry")
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(503))
                .willSetStateTo("third"));
        stubFor(get("/retry")
                .inScenario("retry")
                .whenScenarioStateIs("third")
                .willReturn(aResponse().withStatus(200).withBody("OK")));

        RestClientProperties.Resilience resilience = RestClientProperties.Resilience.builder()
                .retryEnabled(true)
                .retry(RestClientProperties.RetryWrapper.builder()
                        .config(io.github.resilience4j.retry.RetryConfig.custom()
                                .maxAttempts(3)
                                .build())
                        .retryStatus(Set.of(HttpStatus.SERVICE_UNAVAILABLE))
                        .build())
                .build();

        RestClientProperties props = RestClientProperties.builder()
                .baseUrl(baseUrl)
                .resilience(resilience)
                .build();
        RestClient client = builder.client("retryClient", props).build();

        String result = client.get().uri("/retry").retrieve().body(String.class);

        assertEquals("OK", result);
    }

    @Test
    void circuitBreakerShouldOpenAfterFailures() {
        stubFor(get("/cb").willReturn(aResponse().withStatus(500)));

        CircuitBreakerProperties.InstanceProperties cbProps = new CircuitBreakerProperties.InstanceProperties();
        cbProps.setFailureRateThreshold(50.0f);
        cbProps.setMinimumNumberOfCalls(2);
        cbProps.setSlidingWindowSize(2);
        cbProps.setSlidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED);

        RestClientProperties.Resilience resilience = RestClientProperties.Resilience.builder()
                .circuitBreakerEnabled(true)
                .circuitBreaker(cbProps)
                .build();

        RestClientProperties props = RestClientProperties.builder()
                .baseUrl(baseUrl)
                .resilience(resilience)
                .build();
        RestClient client = builder.client("cbClient", props).build();

        // first two calls result in HTTP 500
        assertThrows(
                RestClientResponseException.class,
                () -> client.get().uri("/cb").retrieve().body(String.class));
        assertThrows(
                RestClientResponseException.class,
                () -> client.get().uri("/cb").retrieve().body(String.class));

        // third call should be short‐circuited
        assertThrows(
                CallNotPermittedException.class,
                () -> client.get().uri("/cb").retrieve().body(String.class));
    }

    @Test
    void rateLimiterShouldRejectSecondCall() {
        stubFor(get("/rl").willReturn(aResponse().withStatus(200).withBody("hi")));

        RateLimiterProperties.InstanceProperties rlProps = new RateLimiterProperties.InstanceProperties();
        rlProps.setLimitForPeriod(1);
        rlProps.setLimitRefreshPeriod(Duration.ofSeconds(10));
        rlProps.setTimeoutDuration(Duration.ZERO);

        RestClientProperties.Resilience resilience = RestClientProperties.Resilience.builder()
                .rateLimiterEnabled(true)
                .rateLimiter(rlProps)
                .build();

        RestClientProperties props = RestClientProperties.builder()
                .baseUrl(baseUrl)
                .resilience(resilience)
                .build();
        RestClient client = builder.client("rlClient", props).build();

        // first call succeeds
        String first = client.get().uri("/rl").retrieve().body(String.class);
        assertEquals("hi", first);

        // immediate second call should be rate‐limited
        assertThrows(
                RequestNotPermitted.class,
                () -> client.get().uri("/rl").retrieve().body(String.class));
    }

    @Test
    void slowResponseShouldTriggerReadTimeout() {
        stubFor(get("/slow")
                .willReturn(aResponse().withStatus(200).withFixedDelay(500).withBody("slow")));

        HttpClientProperties httpProps = HttpClientProperties.builder()
                .requestFactory(HttpClientProperties.RequestFactory.builder()
                        .readTimeout(Duration.ofMillis(200))
                        .build())
                .build();

        RestClientProperties props = RestClientProperties.builder()
                .baseUrl(baseUrl)
                .httpClient(httpProps)
                .build();

        RestClient client = builder.client("timeoutClient", props).build();

        assertThrows(
                RestClientException.class,
                () -> client.get().uri("/slow").retrieve().body(String.class));
    }

    @Test
    void longerReadTimeoutShouldSucceedOnSlowResponse() {
        // Stub the same 500 ms delay
        wm.stubFor(WireMock.get("/slow-ok")
                .willReturn(
                        WireMock.aResponse().withStatus(200).withFixedDelay(500).withBody("OK")));

        // Set readTimeout to 1s
        HttpClientProperties httpProps = HttpClientProperties.builder()
                .requestFactory(HttpClientProperties.RequestFactory.builder()
                        .readTimeout(Duration.ofSeconds(1))
                        .build())
                .build();

        RestClientProperties props = RestClientProperties.builder()
                .baseUrl(baseUrl)
                .httpClient(httpProps)
                .build();

        RestClient client = builder.client("noTimeoutClient", props).build();

        // Call should succeed within 1 second
        String body = client.get().uri("/slow-ok").retrieve().body(String.class);

        assertEquals("OK", body, "Expected successful invocation with body 'OK'");
    }

    @Test
    void concurrentRequestsUnderLimitsShouldAllSucceed() throws Exception {
        // lower delay to 50ms so 20 threads finish well within timeout
        wm.stubFor(get("/fast")
            .willReturn(aResponse()
                .withFixedDelay(50)
                .withStatus(200)
                .withBody("OK")));

        HttpClientProperties.Pool pool = HttpClientProperties.Pool.builder()
            .maxTotalConnections(20)
            .maxConnectionsPerRoute(20)
            .build();

        HttpClientProperties httpProps = HttpClientProperties.builder()
            .pool(pool)
            .build();

        RestClientProperties props = RestClientProperties.builder()
            .baseUrl(baseUrl)
            .httpClient(httpProps)
            .build();

        RestClient client = builder.client("concurrentOK", props).build();

        int threads = 20;
        ExecutorService exec = Executors.newFixedThreadPool(threads);
        List<Future<String>> futures = new ArrayList<>(threads);

        for (int i = 0; i < threads; i++) {
            futures.add(exec.submit(() ->
                client.get().uri("/fast").retrieve().body(String.class)
            ));
        }

        // give up to 3 seconds for each future
        for (Future<String> f : futures) {
            assertThat(f.get(3, TimeUnit.SECONDS))
                .as("each concurrent response")
                .isEqualTo("OK");
        }

        exec.shutdownNow();
        assertThat(exec.awaitTermination(1, TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void singleConnectionPoolForcesSequentialExecution() throws Exception {
        int delayMs = 300;
        wm.stubFor(get("/slow")
            .willReturn(aResponse()
                .withFixedDelay(delayMs)
                .withStatus(200)
                .withBody("OK")));

        // Pool size = 1 connection
        HttpClientProperties.Pool pool = HttpClientProperties.Pool.builder()
            .maxTotalConnections(1)
            .maxConnectionsPerRoute(1)
            .build();

        var httpProps = HttpClientProperties.builder()
            .pool(pool)
            .build();

        var props = RestClientProperties.builder()
            .baseUrl(baseUrl)
            .httpClient(httpProps)
            .build();

        RestClient client = builder.client("sequential", props).build();

        ExecutorService exec = Executors.newFixedThreadPool(2);

        Callable<String> task = () ->
            client.get().uri("/slow").retrieve().body(String.class);

        // Measure wall‐clock time for both calls
        long start = System.nanoTime();
        Future<String> f1 = exec.submit(task);
        Future<String> f2 = exec.submit(task);

        // Both should return "OK"
        assertThat(f1.get(2, TimeUnit.SECONDS)).isEqualTo("OK");
        assertThat(f2.get(2, TimeUnit.SECONDS)).isEqualTo("OK");
        long elapsedNanos = System.nanoTime() - start;

        exec.shutdown();
        assertThat(exec.awaitTermination(1, TimeUnit.SECONDS)).isTrue();

        long minExpected = TimeUnit.MILLISECONDS.toNanos(delayMs * 2);
        // Allow a bit of overhead but ensure it's at least 2× the delay
        assertThat(elapsedNanos)
            .as("Total elapsed for two sequential calls")
            .isGreaterThanOrEqualTo(minExpected);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("scenarios")
    void runScenario(String name, Scenario sc) {
        // stub
        sc.stubSetup.run();

        // build props
        var pb = RestClientProperties.builder().baseUrl(baseUrl);
        sc.propsMutator.accept(pb);
        RestClientProperties props = pb.build();

        // build client
        RestClient client = builder.client(name, props).build();

        // invoke & assert
        if (sc.expectException != null) {
            assertThrows(sc.expectException, () -> sc.invoke.apply(client), "Scenario " + name + " expected exception");
        } else {
            String body = sc.invoke.apply(client);
            assertEquals(sc.expectBody, body, "Scenario " + name + " returned");
        }
    }

    static class Scenario {
        final String name;
        final Consumer<RestClientProperties.RestClientPropertiesBuilder> propsMutator;
        final Runnable stubSetup;
        final Function<RestClient, String> invoke;
        final Class<? extends Throwable> expectException;
        final String expectBody;

        Scenario(
                String name,
                Consumer<RestClientProperties.RestClientPropertiesBuilder> propsMutator,
                Runnable stubSetup,
                Function<RestClient, String> invoke,
                Class<? extends Throwable> expectException,
                String expectBody) {
            this.name = name;
            this.propsMutator = propsMutator;
            this.stubSetup = stubSetup;
            this.invoke = invoke;
            this.expectException = expectException;
            this.expectBody = expectBody;
        }
    }

    static Stream<Arguments> scenarios() {
        return Stream.of(
                // 1) default no-retry → immediate 500
                Arguments.of(
                        "defaultNoRetry",
                        new Scenario(
                                "defaultNoRetry",
                                b -> {},
                                () -> stubFor(
                                        get("/no-retry").willReturn(aResponse().withStatus(500))),
                                client ->
                                        client.get().uri("/no-retry").retrieve().body(String.class),
                                RestClientResponseException.class,
                                null)),

                // 2) retry on status 502 → OK
                Arguments.of(
                        "retryOnStatus",
                        new Scenario(
                                "retryOnStatus",
                                b -> b.resilience(RestClientProperties.Resilience.builder()
                                        .retryEnabled(true)
                                        .retry(RestClientProperties.RetryWrapper.builder()
                                                .config(RetryConfig.custom()
                                                        .maxAttempts(3)
                                                        .build())
                                                .retryStatus(Set.of(HttpStatus.BAD_GATEWAY))
                                                .build())
                                        .build()),
                                () -> {
                                    stubFor(get("/status-retry")
                                            .inScenario("statusRetry")
                                            .whenScenarioStateIs(STARTED)
                                            .willReturn(aResponse().withStatus(502))
                                            .willSetStateTo("second"));
                                    stubFor(get("/status-retry")
                                            .inScenario("statusRetry")
                                            .whenScenarioStateIs("second")
                                            .willReturn(aResponse().withStatus(502))
                                            .willSetStateTo("third"));
                                    stubFor(get("/status-retry")
                                            .inScenario("statusRetry")
                                            .whenScenarioStateIs("third")
                                            .willReturn(
                                                    aResponse().withStatus(200).withBody("OK")));
                                },
                                client -> client.get()
                                        .uri("/status-retry")
                                        .retrieve()
                                        .body(String.class),
                                null,
                                "OK")),

                // 3) retry on exception → OK
                Arguments.of(
                        "retryOnException",
                        new Scenario(
                                "retryOnException",
                                b -> b.resilience(RestClientProperties.Resilience.builder()
                                        .retryEnabled(true)
                                        .retry(RestClientProperties.RetryWrapper.builder()
                                                .config(RetryConfig.custom()
                                                        .maxAttempts(2)
                                                        .build())
                                                .build())
                                        .build()),
                                () -> {
                                    stubFor(get("/ex-retry")
                                            .inScenario("exRetry")
                                            .whenScenarioStateIs(STARTED)
                                            .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                                            .willSetStateTo("ok"));
                                    stubFor(get("/ex-retry")
                                            .inScenario("exRetry")
                                            .whenScenarioStateIs("ok")
                                            .willReturn(
                                                    aResponse().withStatus(200).withBody("OK")));
                                },
                                client ->
                                        client.get().uri("/ex-retry").retrieve().body(String.class),
                                null,
                                "OK")),

                // 4) retry exhausted then CB opens
                Arguments.of(
                        "retryExhaustThenCbOpen",
                        new Scenario(
                                "retryExhaustThenCbOpen",
                                b -> b.resilience(RestClientProperties.Resilience.builder()
                                        .retryEnabled(true)
                                        .circuitBreakerEnabled(true)
                                        .retry(RestClientProperties.RetryWrapper.builder()
                                                .config(RetryConfig.custom()
                                                        .maxAttempts(2)
                                                        .build())
                                                .retryStatus(Set.of(HttpStatus.INTERNAL_SERVER_ERROR))
                                                .build())
                                        .circuitBreaker(newCbProps(2, 50.0f, Duration.ofMillis(200)))
                                        .build()),
                                () -> stubFor(
                                        get("/cb-open").willReturn(aResponse().withStatus(500))),
                                client -> {
                                    // first two calls → HTTP 500
                                    try {
                                        client.get().uri("/cb-open").retrieve().body(String.class);
                                    } catch (RestClientResponseException ignore) {
                                    }
                                    try {
                                        client.get().uri("/cb-open").retrieve().body(String.class);
                                    } catch (RestClientResponseException ignore) {
                                    }
                                    // third call → CB short-circuits
                                    return client.get()
                                            .uri("/cb-open")
                                            .retrieve()
                                            .body(String.class);
                                },
                                CallNotPermittedException.class,
                                null)),

                // 5) CB half-open → closed on success
                Arguments.of(
                        "cbRecoverHalfOpenToClosed",
                        new Scenario(
                                "cbRecover",
                                b -> b.resilience(RestClientProperties.Resilience.builder()
                                        .retryEnabled(false)
                                        .circuitBreakerEnabled(true)
                                        .circuitBreaker(newCbProps(2, 50.0f, Duration.ofMillis(200)))
                                        .build()),
                                () -> {
                                    stubFor(get("/cb-recover")
                                            .inScenario("recover")
                                            .whenScenarioStateIs(STARTED)
                                            .willReturn(aResponse().withStatus(500))
                                            .willSetStateTo("ok"));
                                    stubFor(get("/cb-recover")
                                            .inScenario("recover")
                                            .whenScenarioStateIs("ok")
                                            .willReturn(
                                                    aResponse().withStatus(200).withBody("RECOVER")));
                                },
                                client -> {
                                    // two failures → open
                                    try {
                                        client.get()
                                                .uri("/cb-recover")
                                                .retrieve()
                                                .body(String.class);
                                    } catch (RestClientResponseException ignore) {
                                    }
                                    try {
                                        client.get()
                                                .uri("/cb-recover")
                                                .retrieve()
                                                .body(String.class);
                                    } catch (RestClientResponseException ignore) {
                                    }
                                    // next short-circuit
                                    try {
                                        client.get()
                                                .uri("/cb-recover")
                                                .retrieve()
                                                .body(String.class);
                                    } catch (CallNotPermittedException ignore) {
                                    }
                                    // wait for transition to half-open
                                    try {
                                        Thread.sleep(300);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        throw new RuntimeException(ie);
                                    }
                                    // then recover
                                    return client.get()
                                            .uri("/cb-recover")
                                            .retrieve()
                                            .body(String.class);
                                },
                                null,
                                "RECOVER")),

                // 6) rate limiter throttles
                Arguments.of(
                        "rateLimitThrottle",
                        new Scenario(
                                "rateLimitThrottle",
                                b -> b.resilience(RestClientProperties.Resilience.builder()
                                        .rateLimiterEnabled(true)
                                        .rateLimiter(newRlProps(1, Duration.ofSeconds(10), Duration.ZERO))
                                        .build()),
                                () -> stubFor(get("/rl-throttle")
                                        .willReturn(aResponse().withStatus(200))),
                                client -> {
                                    client.get().uri("/rl-throttle").retrieve().body(String.class);
                                    return client.get()
                                            .uri("/rl-throttle")
                                            .retrieve()
                                            .body(String.class);
                                },
                                RequestNotPermitted.class,
                                null)),

                // 7) rate limiter recovers
                Arguments.of(
                        "rateLimitRecovery",
                        new Scenario(
                                "rateLimitRecovery",
                                b -> b.resilience(RestClientProperties.Resilience.builder()
                                        .rateLimiterEnabled(true)
                                        .rateLimiter(newRlProps(1, Duration.ofMillis(200), Duration.ZERO))
                                        .build()),
                                () -> stubFor(get("/rl-recover")
                                        .willReturn(aResponse().withStatus(200).withBody("HI"))),
                                client -> {
                                    client.get().uri("/rl-recover").retrieve().body(String.class);
                                    try {
                                        Thread.sleep(300);
                                    } catch (InterruptedException ie) {
                                        Thread.currentThread().interrupt();
                                        throw new RuntimeException(ie);
                                    }
                                    return client.get()
                                            .uri("/rl-recover")
                                            .retrieve()
                                            .body(String.class);
                                },
                                null,
                                "HI")));
    }

    private static CircuitBreakerProperties.InstanceProperties newCbProps(
            int size, float failureRateThreshold, Duration wait) {
        CircuitBreakerProperties.InstanceProperties cb = new CircuitBreakerProperties.InstanceProperties();
        cb.setMinimumNumberOfCalls(size);
        cb.setSlidingWindowSize(size);
        cb.setFailureRateThreshold(failureRateThreshold);
        cb.setWaitDurationInOpenState(wait);
        return cb;
    }

    private static RateLimiterProperties.InstanceProperties newRlProps(
            int limitForPeriod, Duration refreshPeriod, Duration timeout) {
        RateLimiterProperties.InstanceProperties rl = new RateLimiterProperties.InstanceProperties();
        rl.setLimitForPeriod(limitForPeriod);
        rl.setLimitRefreshPeriod(refreshPeriod);
        rl.setTimeoutDuration(timeout);
        return rl;
    }
}
