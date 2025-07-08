package com.example.http.client.builder;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.http.client.property.HttpClientProperties;
import com.example.http.client.property.HttpClientProperties.Store;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SslContextBuilderTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldReturnNullWhenSslIsDisabled() {
        HttpClientProperties.Ssl ssl =
                HttpClientProperties.Ssl.builder().enabled(false).build();

        SSLContext context = SslContextBuilder.from(ssl).build();

        assertThat(context).isNull();
    }

    @Test
    void shouldBuildTrustAllSslContext() {
        HttpClientProperties.Ssl ssl =
                HttpClientProperties.Ssl.builder().enabled(true).trustAll(true).build();

        SSLContext context = SslContextBuilder.from(ssl).build();

        assertThat(context).isNotNull();
    }

    @Test
    void shouldSkipKeystoreAndTruststoreIfIncomplete() {
        HttpClientProperties.Ssl ssl = HttpClientProperties.Ssl.builder()
                .enabled(true)
                .trustAll(false)
                .truststore(new Store())
                .keystore(new Store())
                .build();

        SSLContext context = SslContextBuilder.from(ssl).build();

        assertThat(context).isNotNull();
    }

    @Test
    void shouldBuildValidSslContextWithMockedStores() {
        HttpClientProperties.Ssl ssl = HttpClientProperties.Ssl.builder()
                .enabled(true)
                .trustAll(false)
                .truststore(Store.builder()
                        .location("classpath:ssl/truststore.p12")
                        .password("changeit")
                        .type("PKCS12")
                        .provider("SunJSSE")
                        .build())
                .keystore(Store.builder()
                        .location("classpath:ssl/keystore.p12")
                        .password("changeit")
                        .type("PKCS12")
                        .provider("SunJSSE")
                        .build())
                .build();

        SSLContext context = SslContextBuilder.from(ssl).build();

        assertThat(context).isNotNull();
    }

    @Test
    void shouldInvokeValidatorWhenProvided() {
        HttpClientProperties.Ssl ssl =
                HttpClientProperties.Ssl.builder().enabled(true).trustAll(true).build();

        SSLContext context = SslContextBuilder.from(ssl, validator).build();

        assertThat(context).isNotNull();
    }
}
