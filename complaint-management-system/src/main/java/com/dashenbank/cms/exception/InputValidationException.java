package com.dashenbank.cms.exception;

import java.util.Map;

public class InputValidationException extends RuntimeException {
    private final Map<String, String> errors;

    public InputValidationException(Map<String, String> errors) {
        super("Validation failed");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
