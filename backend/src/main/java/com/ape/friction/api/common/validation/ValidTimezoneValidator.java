package com.ape.friction.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.ZoneId;

public class ValidTimezoneValidator implements ConstraintValidator<ValidTimezone, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return true;
        return ZoneId.getAvailableZoneIds().contains(value);
    }
}
