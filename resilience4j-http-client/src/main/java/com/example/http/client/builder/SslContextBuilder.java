package com.example.http.client.builder;

import com.example.http.client.property.HttpClientProperties;
import com.example.http.client.util.ResourceUtils;
import com.example.http.client.validation.SslValidator;
import jakarta.validation.Validator;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.Objects;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContexts;

@Slf4j
public class SslContextBuilder {

    private final HttpClientProperties.Ssl ssl;
    private final Validator validator;

    private SslContextBuilder(HttpClientProperties.Ssl ssl, Validator validator) {
        this.ssl = ssl;
        this.validator = validator;
    }

    public static SslContextBuilder from(HttpClientProperties.Ssl ssl) {
        return new SslContextBuilder(ssl, null);
    }

    public static SslContextBuilder from(HttpClientProperties.Ssl ssl, Validator validator) {
        return new SslContextBuilder(ssl, validator);
    }

    public SSLContext build() {
        if (!ssl.isEnabled()) {
            log.debug("SSL is disabled; no SSLContext will be built");
            return null;
        }

        if (Objects.nonNull(validator)) {
            SslValidator.validateStores(ssl, validator);
        }

        try {
            if (ssl.isTrustAll()) {
                log.warn("SSL trustAll is enabled — all certificates will be accepted");
                return SSLContexts.custom()
                        .loadTrustMaterial(null, TrustAllStrategy.INSTANCE)
                        .build();
            }

            var customSsl = SSLContexts.custom();

            KeyStore trustStore = loadKeyStore(ssl.getTruststore(), "truststore");
            if (trustStore != null) {
                customSsl.loadTrustMaterial(trustStore, null);
            }

            KeyStore keyStore = loadKeyStore(ssl.getKeystore(), "keystore");
            if (keyStore != null) {
                customSsl.loadKeyMaterial(keyStore, getPassword(ssl.getKeystore()));
            }

            return customSsl.build();

        } catch (Exception e) {
            throw new RuntimeException("Failed to build SSL context due to " + e.getMessage(), e);
        }
    }

    private KeyStore loadKeyStore(HttpClientProperties.Store store, String label) throws Exception {
        if (store == null || store.getLocation() == null || store.getPassword() == null || store.getType() == null) {
            log.debug("Skipping {}: incomplete configuration", label);
            return null;
        }

        log.info("Loading {} from location: {}", label, store.getLocation());

        try {
            KeyStore keyStore = (store.getProvider() != null)
                    ? KeyStore.getInstance(store.getType(), store.getProvider())
                    : KeyStore.getInstance(store.getType());

            try (InputStream is = ResourceUtils.resolveStream(store.getLocation())) {
                keyStore.load(is, store.getPassword().toCharArray());
            }

            return keyStore;

        } catch (Exception e) {
            log.error("Failed to load {} from {}: {}", label, store.getLocation(), e.getMessage());
            throw e;
        }
    }

    private char[] getPassword(HttpClientProperties.Store store) {
        return (store != null && store.getPassword() != null)
                ? store.getPassword().toCharArray()
                : null;
    }
}
