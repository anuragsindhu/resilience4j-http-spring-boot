package com.example.http.autoconfiguration.utils;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.matching.UrlPattern;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WireMockErrorScenarioBuilder {

    private final int port;

    private WireMockErrorScenarioBuilder(int port) {
        this.port = port;
        WireMock.configureFor("localhost", port);
    }

    public static WireMockErrorScenarioBuilder forPort(int port) {
        return new WireMockErrorScenarioBuilder(port);
    }

    public WireMockErrorScenarioBuilder withBadRequestPost(String path) {
        return stubWithMethod("POST", path, aResponse().withStatus(400).withBody("Bad Request"));
    }

    public WireMockErrorScenarioBuilder withChunkedSlowResponse(String path) {
        return stubWithMethod(
                "GET", path, aResponse().withBody("slow-streamed-response").withChunkedDribbleDelay(5, 10000));
    }

    public WireMockErrorScenarioBuilder withConnectionReset(String path) {
        return stubWithMethod("GET", path, aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER));
    }

    public WireMockErrorScenarioBuilder withEmptyResponse(String path) {
        return stubWithMethod("GET", path, aResponse().withFault(Fault.EMPTY_RESPONSE));
    }

    public WireMockErrorScenarioBuilder withFlakyDNS(String path) {
        return stubWithMethod(
                "GET",
                path,
                aResponse().withFixedDelay(4000).withHeader("X-DNS", "Slow").withBody("DNS slowdown"));
    }

    public WireMockErrorScenarioBuilder withForbiddenPut(String path) {
        return stubWithMethod("PUT", path, aResponse().withStatus(403).withBody("Forbidden"));
    }

    public WireMockErrorScenarioBuilder withHttp500(String path) {
        return stubWithMethod("ANY", path, aResponse().withStatus(500).withBody("Internal Error"));
    }

    public WireMockErrorScenarioBuilder withHttp503(String path) {
        return stubWithMethod(
                "ANY",
                path,
                aResponse().withStatus(503).withHeader("Retry-After", "120").withBody("Service Unavailable"));
    }

    public WireMockErrorScenarioBuilder withMalformedResponse(String path) {
        return stubWithMethod("GET", path, aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK));
    }

    public WireMockErrorScenarioBuilder withNetworkTimeout(String path) {
        return stubWithMethod("GET", path, aResponse().withFixedDelay(10000).withBody("Simulated delay"));
    }

    public WireMockErrorScenarioBuilder withRandomDataThenClose(String path) {
        return stubWithMethod("GET", path, aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE));
    }

    public WireMockErrorScenarioBuilder withRetryableEndpoint(String scenario, String path, int failuresBeforeSuccess) {
        for (int i = 1; i <= failuresBeforeSuccess; i++) {
            String state = "FAIL_" + i;
            String next = (i == failuresBeforeSuccess) ? "SUCCESS" : "FAIL_" + (i + 1);
            stubFor(get(urlEqualTo(path))
                    .inScenario(scenario)
                    .whenScenarioStateIs(i == 1 ? STARTED : state)
                    .willSetStateTo(next)
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withHeader("X-Scenario-State", state)
                            .withBody("Simulated error in state: " + state)));
        }

        stubFor(get(urlEqualTo(path))
                .inScenario(scenario)
                .whenScenarioStateIs("SUCCESS")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("X-Scenario-State", "SUCCESS")
                        .withBody("Recovered")));

        return this;
    }

    public WireMockErrorScenarioBuilder withStateTransitionEndpoint(
            String scenario, String nextState, String triggerPath) {
        stubFor(post(urlEqualTo(triggerPath))
                .inScenario(scenario)
                .whenScenarioStateIs(STARTED)
                .willSetStateTo(nextState)
                .willReturn(aResponse().withStatus(202).withBody("Transitioned to: " + nextState)));
        return this;
    }

    public WireMockErrorScenarioBuilder withStub(
            String method, String path, int status, String body, Map<String, String> headers) {
        ResponseDefinitionBuilder response = aResponse().withStatus(status).withBody(body);
        headers.forEach(response::withHeader);
        log.info("📌 Stubbed [{}] {} → {} with headers {}", method.toUpperCase(), path, status, headers);
        return stubWithMethod(method, path, response);
    }

    public WireMockErrorScenarioBuilder withStub(
            String method, UrlPattern pattern, int status, String body, Map<String, String> headers) {
        ResponseDefinitionBuilder response = aResponse().withStatus(status).withBody(body);
        headers.forEach(response::withHeader);
        stubFor(request(method.toUpperCase(), pattern).willReturn(response));
        return this;
    }

    public void reset() {
        WireMock.reset();
        log.info("🔄 WireMock stubs reset.");
    }

    private WireMockErrorScenarioBuilder stubWithMethod(
            String method, String path, ResponseDefinitionBuilder response) {
        MappingBuilder builder =
                switch (method.toUpperCase()) {
                    case "GET" -> get(urlEqualTo(path));
                    case "POST" -> post(urlEqualTo(path));
                    case "PUT" -> put(urlEqualTo(path));
                    case "PATCH" -> patch(urlEqualTo(path));
                    case "DELETE" -> delete(urlEqualTo(path));
                    case "OPTIONS" -> options(urlEqualTo(path));
                    case "HEAD" -> head(urlEqualTo(path));
                    case "TRACE" -> trace(urlEqualTo(path));
                    case "ANY" -> any(urlEqualTo(path));
                    default -> request(method.toUpperCase(), urlEqualTo(path));
                };
        stubFor(builder.willReturn(response));
        return this;
    }
}
