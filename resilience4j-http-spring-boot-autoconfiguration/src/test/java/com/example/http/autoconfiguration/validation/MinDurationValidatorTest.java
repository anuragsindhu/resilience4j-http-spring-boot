package com.example.http.autoconfiguration.validation;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MinDurationValidatorTest {

    private Validator validator;

    @BeforeEach
    void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void shouldPassValidationWhenDurationIsAboveMinimum() {
        Config cfg = new Config(Duration.ofMillis(1001));
        Set<ConstraintViolation<Config>> violations = validator.validate(cfg);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldPassValidationWhenDurationEqualsMinimum() {
        Config cfg = new Config(Duration.ofMillis(1000));
        Set<ConstraintViolation<Config>> violations = validator.validate(cfg);
        assertThat(violations).isEmpty();
    }

    @Test
    void shouldFailValidationWhenDurationIsBelowMinimum() {
        Config cfg = new Config(Duration.ofMillis(999));
        Set<ConstraintViolation<Config>> violations = validator.validate(cfg);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Duration must be at least 1000 ms");
    }

    @Test
    void shouldFailValidationWhenDurationIsZero() {
        Config cfg = new Config(Duration.ZERO);
        Set<ConstraintViolation<Config>> violations = validator.validate(cfg);
        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldFailValidationWhenDurationIsNegative() {
        Config cfg = new Config(Duration.ofMillis(-5));
        Set<ConstraintViolation<Config>> violations = validator.validate(cfg);
        assertThat(violations).hasSize(1);
    }

    @Test
    void shouldFailValidationWhenDurationIsNull() {
        Config cfg = new Config(null);
        Set<ConstraintViolation<Config>> violations = validator.validate(cfg);
        assertThat(violations).hasSize(1);
    }

    static class Config {
        @MinDuration(value = 1000)
        Duration waitTime;

        Config(Duration waitTime) {
            this.waitTime = waitTime;
        }
    }
}
