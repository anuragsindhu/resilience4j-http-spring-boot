package com.example.http.autoconfiguration.utils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class WireMockRequestAssertTest {

    private static final String TEST_PATH = "/api/test";

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(WireMockConfiguration.options().dynamicPort())
            .build();

    @BeforeEach
    void resetWiremock() {
        wiremock.resetAll();
    }

    @Test
    void shouldPassWhenRequestWasCalledExpectedTimes() {
        WireMock.configureFor("localhost", wiremock.getPort());
        TestHttpClient.post(wiremock.getRuntimeInfo().getHttpBaseUrl() + TEST_PATH, "hello");

        new WireMockRequestAssert("POST", TEST_PATH).wasCalledTimes(1);
    }

    @Test
    void shouldFailWhenRequestWasCalledUnexpectedNumberOfTimes() {
        WireMock.configureFor("localhost", wiremock.getPort());
        TestHttpClient.get(wiremock.getRuntimeInfo().getHttpBaseUrl() + TEST_PATH);

        assertThatThrownBy(() -> new WireMockRequestAssert("GET", TEST_PATH).wasCalledTimes(2))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("to be called 2 times but was 1");
    }

    @Test
    void shouldPassWhenHeaderIsPresent() {
        WireMock.configureFor("localhost", wiremock.getPort());
        TestHttpClient.post(wiremock.getRuntimeInfo().getHttpBaseUrl() + TEST_PATH, "{}", Map.of("X-Custom", "value"));

        new WireMockRequestAssert("POST", TEST_PATH).hasHeader("X-Custom", "value");
    }

    @Test
    void shouldFailWhenHeaderIsMissing() {
        WireMock.configureFor("localhost", wiremock.getPort());
        TestHttpClient.post(wiremock.getRuntimeInfo().getHttpBaseUrl() + TEST_PATH, "{}", Map.of("X-Other", "wrong"));

        assertThatThrownBy(() -> new WireMockRequestAssert("POST", TEST_PATH).hasHeader("X-Custom", "value"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("Expected header [X-Custom: value]");
    }

    @Test
    void shouldPassWhenBodyContainsExpectedSnippet() {
        WireMock.configureFor("localhost", wiremock.getPort());
        TestHttpClient.post(wiremock.getRuntimeInfo().getHttpBaseUrl() + TEST_PATH, "hello world");

        new WireMockRequestAssert("POST", TEST_PATH).hasBodyContaining("world");
    }

    @Test
    void shouldFailWhenBodyDoesNotContainExpectedSnippet() {
        WireMock.configureFor("localhost", wiremock.getPort());
        TestHttpClient.post(wiremock.getRuntimeInfo().getHttpBaseUrl() + TEST_PATH, "something else");

        assertThatThrownBy(() -> new WireMockRequestAssert("POST", TEST_PATH).hasBodyContaining("missing"))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("to contain: 'missing'");
    }
}
