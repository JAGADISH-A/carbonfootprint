package com.carbonfootprint.platform.ingestion.validation.impl;

import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.IngestionLifecycleStage;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;
import com.carbonfootprint.platform.ingestion.validation.RawDocumentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.context.annotation.Profile;

/**
 * Detects duplicate ingestion attempts.
 *
 * <p>Checks whether an activity derived from this {@link RawDocument} already
 * exists in the repository. Prevents double-counting of the same receipt or
 * email if the user uploads/syncs the same document more than once.
 *
 * <p>Uses {@code rawDocumentId} as the deduplication key. If no rawDocumentId
 * is set (MANUAL entries), the check is skipped.
 */
@Component
@Order(4)
@Profile("!stub")
@RequiredArgsConstructor
public class DuplicateDetectionValidator implements RawDocumentValidator {

    private final ActivityRepository activityRepository;

    @Override
    public ValidationResult validate(RawDocument document, IngestionLifecycleStage stage) {
        if (!StringUtils.hasText(document.getId()) || !StringUtils.hasText(document.getUserId())) {
            return ValidationResult.ok(); // Handled by RequiredFieldsValidator
        }

        boolean isDuplicate = activityRepository
                .existsByUserIdAndRawDocumentId(document.getUserId(), document.getId());

        if (isDuplicate) {
            return ValidationResult.fail(
                    "Duplicate ingestion detected. An activity already exists for "
                            + "rawDocumentId='" + document.getId() + "' and userId='" + document.getUserId() + "'. "
                            + "The document has already been processed."
            );
        }

        return ValidationResult.ok();
    }
}
