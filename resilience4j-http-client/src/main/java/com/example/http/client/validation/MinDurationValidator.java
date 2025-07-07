package com.example.http.client.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.Duration;

public class MinDurationValidator implements ConstraintValidator<MinDuration, Duration> {

    private long minMillis;

    @Override
    public void initialize(MinDuration constraintAnnotation) {
        this.minMillis = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(Duration duration, ConstraintValidatorContext context) {
        return duration != null && duration.toMillis() >= minMillis;
    }
}
