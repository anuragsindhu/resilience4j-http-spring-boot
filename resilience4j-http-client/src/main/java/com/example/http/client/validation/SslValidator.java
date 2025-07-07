package com.example.http.client.validation;

import com.example.http.client.property.HttpClientProperties;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.stream.Collectors;

public final class SslValidator {

    private SslValidator() {
        // utility class
    }

    /**
     * Validates truststore and keystore if any store fields are set.
     *
     * @throws IllegalArgumentException if partial store config is detected
     */
    public static void validateStores(HttpClientProperties.Ssl ssl, Validator validator) {
        if (ssl == null || validator == null) return;

        validateIfPartiallyFilled("truststore", ssl.getTruststore(), validator);
        validateIfPartiallyFilled("keystore", ssl.getKeystore(), validator);
    }

    private static void validateIfPartiallyFilled(String name, HttpClientProperties.Store store, Validator validator) {
        if (store == null) return;

        boolean partial = isFilled(store.getLocation()) || isFilled(store.getPassword()) || isFilled(store.getType());
        if (!partial) return;

        Set<ConstraintViolation<HttpClientProperties.Store>> violations =
                validator.validate(store, SslStoreGroup.class);
        if (!violations.isEmpty()) {
            String msg = violations.stream()
                    .map(v -> name + "." + v.getPropertyPath() + " " + v.getMessage())
                    .collect(Collectors.joining("; "));
            throw new IllegalArgumentException("Invalid SSL store configuration: " + msg);
        }
    }

    private static boolean isFilled(String s) {
        return s != null && !s.isBlank();
    }
}
