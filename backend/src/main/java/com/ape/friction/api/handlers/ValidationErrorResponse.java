// https://reflectoring.io/bean-validation-with-spring-boot/
package com.ape.friction.api.handlers;

import lombok.Data;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class ValidationErrorResponse {

    @Data
    public static class Violation {

        private final String fieldName;
        private final String message;

    }
    @Getter
    private List<Violation> violations = new ArrayList<>();
    public void addViolation(String fieldName, String message) {
        violations.add(new Violation(fieldName, message));
    }

}

