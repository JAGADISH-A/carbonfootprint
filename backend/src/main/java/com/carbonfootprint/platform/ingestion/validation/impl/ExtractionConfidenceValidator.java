package com.carbonfootprint.platform.ingestion.validation.impl;

import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;
import com.carbonfootprint.platform.ingestion.validation.ExtractionResultValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates that the parser confidence meets the minimum acceptable threshold.
 *
 * <h3>Rationale</h3>
 * Low-confidence extractions are likely to produce incorrect activities.
 * Rather than silently creating bad data, the pipeline surfaces this as
 * a validation failure so the document can be flagged for human review.
 *
 * <h3>Configuration</h3>
 * {@code carbon.pipeline.extraction.min-confidence-threshold} (default: 0.3)
 * Set to 0.0 to disable confidence filtering entirely.
 *
 * <h3>Order</h3>
 * Runs first — cheap check before heavier required-fields validation.
 */
@Slf4j
@Component
@Order(1)
public class ExtractionConfidenceValidator implements ExtractionResultValidator {

    private final double minConfidenceThreshold;

    public ExtractionConfidenceValidator(
            @Value("${carbon.pipeline.extraction.min-confidence-threshold:0.3}") double minConfidenceThreshold
    ) {
        this.minConfidenceThreshold = minConfidenceThreshold;
        log.info("ExtractionConfidenceValidator — minConfidenceThreshold={}", minConfidenceThreshold);
    }

    @Override
    public ValidationResult validate(ExtractionResult result) {
        if (minConfidenceThreshold <= 0.0) {
            return ValidationResult.ok(); // Threshold disabled
        }
        if (result.getConfidence() == null) {
            log.debug("ExtractionConfidenceValidator — confidence not reported by parser={}", result.getParserName());
            return ValidationResult.ok(); // Null confidence: parser does not report it; skip check
        }
        if (result.getConfidence() < minConfidenceThreshold) {
            log.warn("ExtractionConfidenceValidator — confidence={} below threshold={} parser={}",
                    result.getConfidence(), minConfidenceThreshold, result.getParserName());
            return ValidationResult.failure(
                    "Extraction confidence " + result.getConfidence()
                            + " is below the minimum threshold of " + minConfidenceThreshold
                            + ". Document may require manual review."
            );
        }
        return ValidationResult.ok();
    }
}
