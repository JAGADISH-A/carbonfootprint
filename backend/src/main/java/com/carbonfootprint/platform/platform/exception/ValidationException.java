package com.carbonfootprint.platform.platform.exception;

import com.carbonfootprint.platform.ingestion.model.ValidationResult;

/**
 * Thrown when the validation layer rejects an input.
 *
 * <p>Carries the full {@link ValidationResult} so callers can access
 * all violation messages, not just the first one.
 *
 * <p>Mapped to HTTP 400 by {@link com.carbonfootprint.platform.platform.web.GlobalExceptionHandler}.
 */
public class ValidationException extends CarbonPlatformException {

    private final ValidationResult validationResult;

    public ValidationException(ValidationResult validationResult) {
        super("Validation failed: " + String.join("; ", validationResult.getViolations()));
        this.validationResult = validationResult;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
