package com.example.http.autoconfiguration.utils;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class WireMockErrorScenarioBuilderTest {

    @RegisterExtension
    static WireMockExtension wireMock =
            WireMockExtension.newInstance().configureStaticDsl(true).build();

    private WireMockErrorScenarioBuilder builder;

    @BeforeEach
    void setUp() {
        builder = WireMockErrorScenarioBuilder.forPort(wireMock.getPort());
        builder.reset();
    }

    @Test
    void shouldStubBadRequestPost() {
        builder.withBadRequestPost("/bad-post");
        assertThatCode(() -> TestHttpClient.post(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/bad-post", ""))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldStubChunkedSlowResponse() {
        builder.withChunkedSlowResponse("/slow-chunk");
        TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/slow-chunk");
    }

    @Test
    void shouldStubConnectionReset() {
        builder.withConnectionReset("/conn-reset");
        TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/conn-reset");
    }

    @Test
    void shouldStubEmptyResponse() {
        builder.withEmptyResponse("/empty");
        TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/empty");
    }

    @Test
    void shouldStubFlakyDNS() {
        builder.withFlakyDNS("/flaky");
        TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/flaky");
    }

    @Test
    void shouldStubForbiddenPut() {
        builder.withForbiddenPut("/forbidden");
        TestHttpClient.put(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/forbidden", "");
    }

    @Test
    void shouldStubHttp500() {
        builder.withHttp500("/five-hundred");
        TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/five-hundred");
    }

    @Test
    void shouldStubHttp503() {
        builder.withHttp503("/unavailable");
        TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/unavailable");
    }

    @Test
    void shouldStubMalformedResponse() {
        builder.withMalformedResponse("/malformed");
        TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/malformed");
    }

    @Test
    void shouldStubNetworkTimeout() {
        builder.withNetworkTimeout("/timeout");
        TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/timeout");
    }

    @Test
    void shouldStubRandomDataThenClose() {
        builder.withRandomDataThenClose("/random-close");
        TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/random-close");
    }

    @Test
    void shouldStubRetryableEndpoint() {
        builder.withRetryableEndpoint("flaky-scenario", "/flaky-call", 3);
        for (int i = 0; i < 4; i++) {
            TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/flaky-call");
        }
    }

    @Test
    void shouldStubStateTransition() {
        builder.withStateTransitionEndpoint("transition", "READY", "/trigger");
        TestHttpClient.post(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/trigger", "");
    }

    @Test
    void shouldStubWithCustomHeadersAndBody() {
        builder.withStub("GET", "/custom", 418, "I'm a teapot", Map.of("X-Mock", "true"));
        TestHttpClient.get(wireMock.getRuntimeInfo().getHttpBaseUrl() + "/custom");
    }
}
