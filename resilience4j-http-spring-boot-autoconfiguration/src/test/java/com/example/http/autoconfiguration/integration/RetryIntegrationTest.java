package com.example.http.autoconfiguration.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.configureFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.ok;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.http.autoconfiguration.TestApplication;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

@SpringBootTest(classes = TestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RetryIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig().dynamicPort())
            .build();

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry reg) {
        reg.add("group.http.clients.retry.base-url", () -> wm.getRuntimeInfo().getHttpBaseUrl());
        reg.add("group.http.clients.retry.resilience.retry-enabled", () -> "true");
        reg.add(
                "group.http.clients.retry.resilience.retry.retry-exceptions[0]",
                () -> "org.apache.hc.core5.http.NoHttpResponseException");
        // retry on network‐drop
        reg.add(
                "group.http.clients.retry.resilience.retry.retry-exceptions[1]",
                () -> "org.springframework.web.client.ResourceAccessException");
        // retry on HTTP 500
        reg.add(
                "group.http.clients.retry.resilience.retry.retry-exceptions[2]",
                () -> "org.springframework.web.client.HttpServerErrorException");
        reg.add("group.http.clients.retry.resilience.retry.max-attempts", () -> "3");
        reg.add("group.http.clients.retry.resilience.retry.wait-duration", () -> "PT0.01S");
    }

    @Autowired
    private Map<String, RestClient> clients;

    @BeforeEach
    void stub() {
        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());

        // 1st attempt: network fault
        stubFor(get("/ping")
                .inScenario("retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE))
                .willSetStateTo("second"));

        // 2nd attempt: HTTP 500
        stubFor(get("/ping")
                .inScenario("retry")
                .whenScenarioStateIs("second")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("third"));

        // 3rd attempt: success
        stubFor(get("/ping").inScenario("retry").whenScenarioStateIs("third").willReturn(ok("pong")));
    }

    @Test
    void retrySucceedsAfterTwoFailures() {
        RestClient client = clients.get("retry");

        String resp = client.get().uri("/ping").retrieve().body(String.class);

        assertThat(resp).isEqualTo("pong");
        wm.verify(3, getRequestedFor(urlEqualTo("/ping")));
    }
}
