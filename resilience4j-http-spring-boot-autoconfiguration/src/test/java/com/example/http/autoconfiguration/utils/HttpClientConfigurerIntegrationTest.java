package com.example.http.autoconfiguration.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static java.util.Map.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;

class HttpClientConfigurerIntegrationTest {

    @RegisterExtension
    static WireMockExtension wiremock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    private TestRestClient client;

    @BeforeEach
    void setUp() {
        client = new TestRestClient(wiremock.getRuntimeInfo().getHttpBaseUrl());
    }

    @Test
    void shouldTimeoutIfServerDelaysBeyondSocketTimeout() {
        wiremock.stubFor(get("/slow").willReturn(aResponse().withFixedDelay(12_000))); // socketTimeout = 10s

        assertThatThrownBy(() -> client.get("/slow", String.class))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("Read timed out");

        wiremock.verify(getRequestedFor(urlEqualTo("/slow")));
    }

    @Test
    void shouldFailFastIfConnectTimeoutExceeded() {
        TestRestClient unreachableClient = new TestRestClient("http://10.255.255.1"); // unroutable IP

        assertThatThrownBy(() -> unreachableClient.get("/", String.class))
                .isInstanceOf(ResourceAccessException.class)
                .hasMessageContaining("Connect timed out");
    }

    @Test
    void shouldReuseConnectionFromPool() {
        wiremock.stubFor(get("/ping").willReturn(ok("pong")));

        for (int i = 0; i < 3; i++) {
            ResponseEntity<String> response = client.get("/ping", String.class);
            assertThat(response.getBody()).isEqualTo("pong");
        }

        wiremock.verify(3, getRequestedFor(urlEqualTo("/ping")));
    }

    @Test
    void shouldSendPostBodyWithTcpNoDelay() {
        wiremock.stubFor(post("/tiny").willReturn(ok("ok")));

        ResponseEntity<String> response = client.post("/tiny", "x", String.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        wiremock.verify(postRequestedFor(urlEqualTo("/tiny")).withRequestBody(equalTo("x")));
    }

    @Test
    void shouldSendCustomHeadersInRawRequest() {
        wiremock.stubFor(post("/raw").willReturn(ok("done")));

        client.rawRequest(HttpMethod.POST, "/raw", "payload", of("X-Test", "true"))
                .toBodilessEntity();

        wiremock.verify(postRequestedFor(urlEqualTo("/raw")).withHeader("X-Test", equalTo("true")));
    }
}
