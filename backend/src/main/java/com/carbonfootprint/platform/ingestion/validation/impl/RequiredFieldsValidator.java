package com.carbonfootprint.platform.ingestion.validation.impl;

import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.IngestionLifecycleStage;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;
import com.carbonfootprint.platform.ingestion.validation.RawDocumentValidator;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates that a {@link RawDocument} contains all required fields.
 *
 * <p>This validator runs first in the chain ({@code @Order(1)}).
 * If required fields are absent, subsequent validators are still
 * called but their results will be merged.
 */
@Component
@Order(1)
public class RequiredFieldsValidator implements RawDocumentValidator {

    @Override
    public ValidationResult validate(RawDocument document, IngestionLifecycleStage stage) {
        List<String> violations = new ArrayList<>();

        if (!StringUtils.hasText(document.getId())) {
            violations.add("RawDocument.id must not be blank.");
        }
        if (document.getSource() == null) {
            violations.add("RawDocument.source must not be null.");
        }
        if (!StringUtils.hasText(document.getUserId())) {
            violations.add("RawDocument.userId must not be blank.");
        }
        // Only require rawText if OCR has completed or if we are beyond the initial reception
        if (stage != IngestionLifecycleStage.RECEIVED && !StringUtils.hasText(document.getRawText())) {
            violations.add("RawDocument.rawText must not be blank — no content was extracted.");
        }
        if (!StringUtils.hasText(document.getMimeType())) {
            violations.add("RawDocument.mimeType must not be blank.");
        }

        return violations.isEmpty()
                ? ValidationResult.ok()
                : ValidationResult.fail(violations);
    }
}
