package com.carbonfootprint.platform.ingestion.validation.impl;

import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.IngestionLifecycleStage;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;
import com.carbonfootprint.platform.ingestion.validation.RawDocumentValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Validates that the {@link RawDocument#getSource()} is a known, accepted source.
 *
 * <p>Prevents ingestion of documents from sources that are not yet
 * supported in this deployment (e.g., rejecting IOT if not configured).
 * The accepted set is currently all non-null sources — this can be
 * restricted to active sources via configuration in future phases.
 */
@Component
@Order(3)
public class CategoryValidator implements RawDocumentValidator {

    /**
     * Sources accepted by the current ingestion pipeline.
     * Future: load this from application configuration.
     */
    private static final Set<ActivitySource> ACCEPTED_SOURCES = Set.of(
            ActivitySource.RECEIPT,
            ActivitySource.GMAIL,
            ActivitySource.SMS,
            ActivitySource.BANK_STATEMENT,
            ActivitySource.MANUAL
    );

    @Override
    public ValidationResult validate(RawDocument document, IngestionLifecycleStage stage) {
        if (document.getSource() == null) {
            return ValidationResult.ok(); // Null source handled by RequiredFieldsValidator
        }

        if (!ACCEPTED_SOURCES.contains(document.getSource())) {
            return ValidationResult.fail(
                    "ActivitySource '" + document.getSource() + "' is not accepted by this pipeline. "
                            + "Accepted sources: " + ACCEPTED_SOURCES
            );
        }

        return ValidationResult.ok();
    }
}
