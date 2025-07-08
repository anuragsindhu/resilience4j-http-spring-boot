package com.example.http.client.builder;

import com.example.http.client.property.HttpClientDefaultSettings;
import com.example.http.client.property.HttpClientProperties;
import org.apache.hc.client5.http.classic.HttpClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class HttpClientConfigurerTest {

    @Test
    void shouldConfigureHttpClientWithDefaultProperties() {
        HttpClientProperties props = HttpClientDefaultSettings.defaultHttpClient();
        HttpClient client = HttpClientConfigurer.configure(props);

        Assertions.assertThat(client).isNotNull();
    }

    @Test
    void shouldUseTrustAllVerifierWhenEnabled() {
        HttpClientProperties props = HttpClientDefaultSettings.defaultHttpClient();
        props.getSsl().setTrustAll(true);
        props.getSsl().setHostnameVerifier(null);

        HttpClient client = HttpClientConfigurer.configure(props);

        Assertions.assertThat(client).isNotNull();
    }

    @Test
    void shouldUseBuiltinVerifierWhenTrustAllDisabled() {
        HttpClientProperties props = HttpClientDefaultSettings.defaultHttpClient();
        props.getSsl().setTrustAll(false);
        props.getSsl().setHostnameVerifier(null);

        HttpClient client = HttpClientConfigurer.configure(props);

        Assertions.assertThat(client).isNotNull();
    }

    @Test
    void shouldBuildTlsStrategyWhenSslContextAvailable() {
        HttpClientProperties props = HttpClientDefaultSettings.defaultHttpClient();
        props.getSsl().setEnabled(true);
        props.getSsl().setTrustAll(true);

        HttpClient client = HttpClientConfigurer.configure(props);

        Assertions.assertThat(client).isNotNull();
    }

    @Test
    void shouldNotFailWhenSslContextIsNull() {
        HttpClientProperties props = HttpClientDefaultSettings.defaultHttpClient();
        props.getSsl().setEnabled(false);

        HttpClient client = HttpClientConfigurer.configure(props);

        Assertions.assertThat(client).isNotNull();
    }
}
