package com.carbonfootprint.platform.ingestion.validation.impl;

import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.IngestionLifecycleStage;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;
import com.carbonfootprint.platform.ingestion.validation.RawDocumentValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Validates temporal consistency of a {@link RawDocument}.
 *
 * <p>Ensures that the {@code createdAt} timestamp is not in the future
 * (within a small clock-skew tolerance). Runs second in the chain.
 */
@Component
@Order(2)
public class TimestampValidator implements RawDocumentValidator {

    /** Acceptable clock skew between client and server (30 seconds). */
    private static final long CLOCK_SKEW_SECONDS = 30L;

    @Override
    public ValidationResult validate(RawDocument document, IngestionLifecycleStage stage) {
        if (document.getCreatedAt() == null) {
            return ValidationResult.ok(); // Missing timestamp handled by RequiredFieldsValidator
        }

        Instant upperBound = Instant.now().plusSeconds(CLOCK_SKEW_SECONDS);
        if (document.getCreatedAt().isAfter(upperBound)) {
            return ValidationResult.fail(
                    "RawDocument.createdAt is in the future: " + document.getCreatedAt()
                            + ". Check client clock synchronisation."
            );
        }

        return ValidationResult.ok();
    }
}
