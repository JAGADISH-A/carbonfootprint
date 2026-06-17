package com.carbonfootprint.platform.ingestion.validation;

import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.IngestionLifecycleStage;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;

/**
 * Validator for {@link RawDocument} objects immediately after ingestion.
 *
 * <p>Implementations are registered as Spring beans and injected as a
 * {@code List<RawDocumentValidator>} into the pipeline service. Each
 * validator has a single, focused responsibility (SRP).
 *
 * <p>The pipeline merges all {@link ValidationResult}s — if any validator
 * fails, the pipeline halts and returns the combined violation messages.
 *
 * <h3>Implementations</h3>
 * <ul>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.impl.RequiredFieldsValidator}</li>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.impl.TimestampValidator}</li>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.impl.CategoryValidator}</li>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.impl.DuplicateDetectionValidator}</li>
 * </ul>
 */
public interface RawDocumentValidator {

    /**
     * Validates the given raw document.
     *
     * @param document the document to validate
     * @param stage    the current lifecycle stage of the document
     * @return a {@link ValidationResult} indicating pass or fail with violation details
     */
    ValidationResult validate(RawDocument document, IngestionLifecycleStage stage);
}
