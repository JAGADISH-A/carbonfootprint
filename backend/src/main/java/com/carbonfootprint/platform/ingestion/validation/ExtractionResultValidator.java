package com.carbonfootprint.platform.ingestion.validation;

import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;

/**
 * Validator for {@link ExtractionResult} objects produced by document parsers.
 *
 * <p>Implementations are registered as Spring beans and injected as a
 * {@code List<ExtractionResultValidator>} into the pipeline service.
 *
 * <h3>Pipeline position</h3>
 * Runs after {@link com.carbonfootprint.platform.ingestion.port.out.DocumentParser}
 * and before {@link com.carbonfootprint.platform.ingestion.normalization.ExtractionResultToActivityConverter}.
 *
 * <h3>Implementations</h3>
 * <ul>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.impl.ExtractionConfidenceValidator}
 *       — rejects extractions below a configurable confidence threshold.</li>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.impl.ExtractionRequiredFieldsValidator}
 *       — rejects extractions missing the minimum required fields.</li>
 * </ul>
 */
public interface ExtractionResultValidator {

    /**
     * Validates the given extraction result.
     *
     * @param result the extraction result to validate
     * @return a {@link ValidationResult} indicating pass or fail with violation messages
     */
    ValidationResult validate(ExtractionResult result);
}
