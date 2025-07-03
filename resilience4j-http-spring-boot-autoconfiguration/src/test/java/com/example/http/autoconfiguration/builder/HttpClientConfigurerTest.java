package com.example.http.autoconfiguration.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.http.autoconfiguration.properties.HttpClientProperties;
import com.example.http.autoconfiguration.properties.HttpClientProperties.Ssl;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.util.ReflectionTestUtils;

class HttpClientConfigurerTest {

    @Test
    void configureThrowsWhenDisabled() {
        HttpClientProperties props =
                HttpClientProperties.builder().enabled(false).build();

        assertThatThrownBy(() -> HttpClientConfigurer.configure(props))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("HttpClient configuration is disabled.");
    }

    @Test
    void configureWithSslDisabledUsesDefaultTimeouts() {
        HttpClientProperties props = HttpClientProperties.builder().build();

        HttpComponentsClientHttpRequestFactory factory = HttpClientConfigurer.configure(props);

        Long connectTimeoutMs = (Long) ReflectionTestUtils.getField(factory, "connectTimeout");
        Long requestTimeoutMs = (Long) ReflectionTestUtils.getField(factory, "connectionRequestTimeout");
        Long readTimeoutMs = (Long) ReflectionTestUtils.getField(factory, "readTimeout");

        assertThat(Duration.ofMillis(connectTimeoutMs)).isEqualTo(Duration.ofSeconds(5));
        assertThat(Duration.ofMillis(requestTimeoutMs)).isEqualTo(Duration.ofSeconds(2));
        assertThat(Duration.ofMillis(readTimeoutMs)).isEqualTo(Duration.ofSeconds(10));

        assertThat(factory.getHttpClient()).isNotNull();
    }

    @Test
    void configureAppliesCustomRequestFactoryTimeouts() {
        HttpClientProperties.RequestFactory customRf = HttpClientProperties.RequestFactory.builder()
                .connectTimeout(Duration.ofMillis(150))
                .connectionRequestTimeout(Duration.ofMillis(250))
                .readTimeout(Duration.ofMillis(350))
                .build();

        HttpClientProperties props =
                HttpClientProperties.builder().requestFactory(customRf).build();

        HttpComponentsClientHttpRequestFactory factory = HttpClientConfigurer.configure(props);

        Long connectTimeoutMs = (Long) ReflectionTestUtils.getField(factory, "connectTimeout");
        Long requestTimeoutMs = (Long) ReflectionTestUtils.getField(factory, "connectionRequestTimeout");
        Long readTimeoutMs = (Long) ReflectionTestUtils.getField(factory, "readTimeout");

        assertThat(Duration.ofMillis(connectTimeoutMs)).isEqualTo(Duration.ofMillis(150));
        assertThat(Duration.ofMillis(requestTimeoutMs)).isEqualTo(Duration.ofMillis(250));
        assertThat(Duration.ofMillis(readTimeoutMs)).isEqualTo(Duration.ofMillis(350));
    }

    @Test
    void configureWithTrustAllSslEnabledSucceeds() {
        HttpClientProperties props = HttpClientProperties.builder()
                .ssl(Ssl.builder().enabled(true).trustAll(true).build())
                .build();

        HttpComponentsClientHttpRequestFactory factory = HttpClientConfigurer.configure(props);

        assertThat(factory.getHttpClient()).isNotNull();
    }

    @Test
    void configureSslEnabledWithBadKeyStoreThrowsRuntimeException() {
        HttpClientProperties props = HttpClientProperties.builder()
                .ssl(Ssl.builder()
                        .enabled(true)
                        .trustAll(false)
                        .trustStorePath("nonexistent.jks")
                        .trustStorePassword("pw")
                        .keyStorePath("nonexistent.jks")
                        .keyStorePassword("pw")
                        .build())
                .build();

        assertThatThrownBy(() -> HttpClientConfigurer.configure(props))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to configure SSL context")
                .hasRootCauseInstanceOf(Exception.class);
    }
}
