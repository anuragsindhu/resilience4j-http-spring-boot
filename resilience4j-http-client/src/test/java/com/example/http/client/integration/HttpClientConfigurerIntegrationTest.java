package com.example.http.client.integration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.example.http.client.builder.HttpClientConfigurer;
import com.example.http.client.property.HttpClientProperties;
import com.example.http.client.property.HttpClientProperties.Ssl;
import com.example.http.client.property.HttpClientProperties.Store;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.ssl.HostnameVerificationPolicy;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class HttpClientConfigurerIntegrationTest {

    @RegisterExtension
    static WireMockExtension wm = WireMockExtension.newInstance()
            .options(WireMockConfiguration.wireMockConfig()
                    .dynamicPort()
                    .dynamicHttpsPort()
                    .keystorePath("src/test/resources/wiremock-keystore.p12")
                    .keystoreType("PKCS12")
                    .keyManagerPassword("changeit")
                    .keystorePassword("changeit"))
            .build();

    private String wireMockHttpUrl;
    private String wireMockHttpsUrl;

    @BeforeEach
    void setupStub() {
        wireMockHttpUrl = wm.getRuntimeInfo().getHttpBaseUrl();
        wireMockHttpsUrl = "https://localhost:" + wm.getRuntimeInfo().getHttpsPort();
        configureFor("localhost", wm.getRuntimeInfo().getHttpPort());

        stubFor(get("/ping").willReturn(ok().withBody("pong")));
        stubFor(get("/delayed").willReturn(aResponse().withFixedDelay(2000).withBody("slow")));
        stubFor(get("/proxy-test").willReturn(ok().withBody("via-proxy")));
        stubFor(get("/secure-ping").willReturn(ok().withBody("secure pong")));
    }

    @Test
    void shouldExecuteConcurrentRequestsWithinPoolLimit() throws Exception {
        HttpClientProperties props = new HttpClientProperties();
        props.getPool().setMaxTotalConnections(2);
        props.getPool().setMaxConnectionsPerRoute(2);

        HttpClient client = HttpClientConfigurer.configure(props);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        Callable<String> task = () -> {
            HttpGet request = new HttpGet(wireMockHttpUrl + "/ping");
            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(request)) {
                return EntityUtils.toString(response.getEntity());
            }
        };

        Future<String> r1 = executor.submit(task);
        Future<String> r2 = executor.submit(task);

        assertThat(r1.get()).isEqualTo("pong");
        assertThat(r2.get()).isEqualTo("pong");
        executor.shutdown();
    }

    @Test
    void shouldEnforceConnectionTimeout() {
        HttpClientProperties props = new HttpClientProperties();
        props.getPool().getConnection().setConnectTimeout(Duration.ofMillis(300));

        HttpClient client = HttpClientConfigurer.configure(props);
        HttpGet request = new HttpGet("http://10.255.255.1/timeout");

        long start = System.currentTimeMillis();
        assertThrows(IOException.class, () -> {
            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(request)) {
                // unreachable host
            }
        });
        long elapsed = System.currentTimeMillis() - start;

        assertThat(elapsed).isLessThan(2000);
    }

    @Test
    void shouldEnforceSocketTimeoutOnSlowResponse() {
        HttpClientProperties props = new HttpClientProperties();
        props.getPool().getSocket().setSoTimeout(Duration.ofMillis(500));

        HttpClient client = HttpClientConfigurer.configure(props);
        HttpGet request = new HttpGet(wireMockHttpUrl + "/delayed");

        assertThrows(IOException.class, () -> {
            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(request)) {
                // slow endpoint will timeout
            }
        });
    }

    @Test
    void shouldServeStubbedResponseDirectlyFromWireMock() throws Exception {
        HttpClientProperties props = new HttpClientProperties();
        HttpClient client = HttpClientConfigurer.configure(props);
        HttpGet request = new HttpGet(wireMockHttpUrl + "/proxy-test");

        try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(request)) {
            assertThat(EntityUtils.toString(response.getEntity())).isEqualTo("via-proxy");
        }
    }

    @Test
    void shouldEvictIdleConnections() throws Exception {
        HttpClientProperties props = new HttpClientProperties();
        props.getPool().setMaxTotalConnections(1);
        props.getPool().getConnection().setIdleEvictionTimeout(Duration.ofMillis(200));

        HttpClient client = HttpClientConfigurer.configure(props);
        HttpGet request = new HttpGet(wireMockHttpUrl + "/ping");

        try (CloseableHttpResponse r1 = (CloseableHttpResponse) client.execute(request)) {
            assertThat(r1.getCode()).isEqualTo(200);
        }

        Thread.sleep(300);

        try (CloseableHttpResponse r2 = (CloseableHttpResponse) client.execute(request)) {
            assertThat(r2.getCode()).isEqualTo(200);
        }
    }

    @Test
    void shouldAllowSslConnectionWithTruststoreAndRelaxedPolicy() throws Exception {
        HttpClientProperties props = new HttpClientProperties();
        props.setSsl(Ssl.builder()
                .enabled(true)
                .hostnameVerificationPolicy(HostnameVerificationPolicy.CLIENT)
                .hostnameVerifier((host, session) -> true)
                .truststore(Store.builder()
                        .location("src/test/resources/wiremock-truststore.jks")
                        .password("changeit")
                        .type("JKS")
                        .build())
                .build());

        HttpClient client = HttpClientConfigurer.configure(props);
        HttpGet request = new HttpGet(wireMockHttpsUrl + "/secure-ping");

        try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(request)) {
            assertThat(response.getCode()).isEqualTo(200);
            assertThat(EntityUtils.toString(response.getEntity())).isEqualTo("secure pong");
        }
    }

    @Test
    void shouldRejectSslConnectionWithBuiltinHostnamePolicy() {
        HttpClientProperties props = new HttpClientProperties();

        Store truststore = Store.builder()
                .location("src/test/resources/wiremock-truststore.jks")
                .password("changeit")
                .type("JKS")
                .provider("SUN")
                .build();

        Ssl ssl = props.getSsl();
        ssl.setEnabled(true);
        ssl.setTrustAll(false);
        ssl.setTruststore(truststore);
        ssl.setHostnameVerificationPolicy(HostnameVerificationPolicy.BUILTIN);

        HttpClient client = HttpClientConfigurer.configure(props);
        HttpGet request = new HttpGet(wireMockHttpsUrl + "/secure-ping");

        assertThrows(IOException.class, () -> {
            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(request)) {
                // host mismatch with CN will fail
            }
        });
    }

    @Test
    void shouldRejectSslConnectionWhenCustomHostnameVerifierFails() {
        HttpClientProperties props = new HttpClientProperties();

        Store truststore = Store.builder()
                .location("src/test/resources/wiremock-truststore.jks")
                .password("changeit")
                .type("JKS")
                .provider("SUN")
                .build();

        Ssl ssl = props.getSsl();
        ssl.setEnabled(true);
        ssl.setTrustAll(false);
        ssl.setTruststore(truststore);
        ssl.setHostnameVerificationPolicy(HostnameVerificationPolicy.CLIENT);
        ssl.setHostnameVerifier((host, session) -> false); // reject all

        HttpClient client = HttpClientConfigurer.configure(props);
        HttpGet request = new HttpGet(wireMockHttpsUrl + "/secure-ping");

        assertThrows(IOException.class, () -> {
            try (CloseableHttpResponse response = (CloseableHttpResponse) client.execute(request)) {
                // handshake blocked by custom verifier
            }
        });
    }
}
