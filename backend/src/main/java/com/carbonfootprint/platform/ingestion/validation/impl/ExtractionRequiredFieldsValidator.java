package com.carbonfootprint.platform.ingestion.validation.impl;

import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;
import com.carbonfootprint.platform.ingestion.validation.ExtractionResultValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates that the extraction result contains the minimum fields required
 * to produce a meaningful {@link com.carbonfootprint.platform.activity.model.Activity}.
 *
 * <h3>Required minimum</h3>
 * An extraction is considered minimally valid if at least one of the following
 * is present:
 * <ul>
 *   <li>A recognized {@code category} (allows categorized assessment even without merchant).</li>
 *   <li>A non-blank {@code merchant} (allows merchant-based lookup even without category).</li>
 * </ul>
 *
 * <h3>Rationale</h3>
 * Activities with no category AND no merchant cannot produce a meaningful
 * carbon assessment. They would require 100% manual correction.
 *
 * <h3>Order</h3>
 * Runs after {@link ExtractionConfidenceValidator} (Order 1).
 */
@Slf4j
@Component
@Order(2)
public class ExtractionRequiredFieldsValidator implements ExtractionResultValidator {

    @Override
    public ValidationResult validate(ExtractionResult result) {
        if (!result.hasMinimumRequiredFields()) {
            log.warn("ExtractionRequiredFieldsValidator — extraction has neither category nor merchant. parser={}",
                    result.getParserName());
            return ValidationResult.failure(
                    "Extraction result must contain at least a category or merchant name. "
                            + "The document may not contain recognizable transaction information."
            );
        }
        return ValidationResult.ok();
    }
}
