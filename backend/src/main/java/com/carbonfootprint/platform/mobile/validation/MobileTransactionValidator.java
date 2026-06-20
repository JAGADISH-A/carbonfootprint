package com.carbonfootprint.platform.mobile.validation;

import com.carbonfootprint.platform.ingestion.model.ValidationResult;
import com.carbonfootprint.platform.mobile.dto.EnrichedTransaction;
import com.carbonfootprint.platform.platform.exception.ValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates the {@link EnrichedTransaction} DTO received from the Android Companion App.
 *
 * <p>Ensures that the incoming data contains the minimum required fields
 * to successfully map to an {@link com.carbonfootprint.platform.activity.model.Activity}
 * and perform carbon calculations.
 */
@Component
public class MobileTransactionValidator {

    public void validateOrThrow(EnrichedTransaction transaction) {
        ValidationResult result = validate(transaction);
        if (!result.isValid()) {
            throw new ValidationException(result);
        }
    }

    public ValidationResult validate(EnrichedTransaction transaction) {
        List<String> violations = new ArrayList<>();

        if (transaction == null) {
            return ValidationResult.fail("Transaction cannot be null");
        }

        if (transaction.getCategory() == null) {
            violations.add("Category is required");
        }

        if (transaction.getAmount() == null && transaction.getUnit() == null) {
            violations.add("Either amount or unit quantity must be provided");
        }

        if (transaction.getAmount() != null && transaction.getAmount().signum() < 0) {
            violations.add("Amount cannot be negative");
        }

        if (violations.isEmpty()) {
            return ValidationResult.ok();
        } else {
            return ValidationResult.fail(violations);
        }
    }
}
