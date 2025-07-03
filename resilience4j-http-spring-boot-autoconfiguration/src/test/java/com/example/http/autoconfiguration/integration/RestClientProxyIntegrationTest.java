package com.example.http.autoconfiguration.integration;

import com.example.http.autoconfiguration.builder.RestClientBuilder;
import com.example.http.autoconfiguration.properties.HttpClientProperties;
import com.example.http.autoconfiguration.properties.RestClientProperties;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

class RestClientProxyIntegrationTest {

//  // Backend service stub
//  @RegisterExtension
//  static WireMockExtension backend = WireMockExtension.newInstance()
//      .options(WireMockConfiguration.wireMockConfig().dynamicPort())
//      .build();
//
//  // Proxy stub (forwards everything to the backend)
//  @RegisterExtension
//  static WireMockExtension proxy = WireMockExtension.newInstance()
//      .options(WireMockConfiguration.wireMockConfig().dynamicPort())
//      .build();
//
//  private RestClient client;
//  private String targetUrl;
//
//  @BeforeEach
//  void setUp() {
//    // reset both stubs
//    backend.resetAll();
//    proxy.resetAll();
//
//    // proxy: forward all requests to backend
//    String backendBase = "http://localhost:" + backend.getPort();
//    proxy.stubFor(any(anyUrl())
//        .willReturn(aResponse()
//            .proxiedFrom(backendBase)
//        ));
//
//    // configure RestClient to use the proxy
//    Proxy proxyProps = Proxy.builder()
//        .host("localhost")
//        .port(proxy.getPort())
//        .build();
//
//    HttpClientProperties httpProps = HttpClientProperties.builder()
//        .proxy(proxyProps)
//        .build();
//
//    RestClientProperties props = RestClientProperties.builder()
//        .baseUrl(backendBase.replace(
//            "localhost:" + backend.getPort(),
//            "localhost:" + backend.getPort()))
//        .httpClient(httpProps)
//        .build();
//
//    client = RestClientBuilder.builder()
//        .observationRegistry(io.micrometer.observation.ObservationRegistry.create())
//        .circuitBreakerRegistry(CircuitBreakerRegistry.ofDefaults())
//        .retryRegistry(RetryRegistry.ofDefaults())
//        .rateLimiterRegistry(RateLimiterRegistry.ofDefaults())
//        .build()
//        .client("viaProxy", props)
//        .build();
//
//    targetUrl = "/hello";
//  }
//
//  @Test
//  void whenUsingProxyAllCallsGoThroughIt() {
//    // backend: expect GET /hello
//    backend.stubFor(get(targetUrl)
//        .willReturn(aResponse()
//            .withStatus(200)
//            .withBody("proxied")
//        ));
//
//    // invoke the client – it should hit the proxy, which in turn hits the backend
//    String result = client.get()
//        .uri(targetUrl)
//        .retrieve()
//        .body(String.class);
//
//    assertThat(result)
//        .as("Response should come from backend via proxy")
//        .isEqualTo("proxied");
//
//    // verify proxy saw a request for the full URL
//    proxy.verify(getRequestedFor(urlMatching(".*" + targetUrl + "$")));
//
//    // verify backend saw exactly one request
//    backend.verify(1, getRequestedFor(equalTo(targetUrl)));
//  }
}