package com.carbonfootprint.platform.ingestion.validation;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;

/**
 * Validator for {@link Activity} objects after parsing and before normalisation.
 *
 * <p>Validates the semantic correctness of the parsed activity (e.g., required
 * fields present, category is not null, amount is positive).
 *
 * <h3>Implementations</h3>
 * <ul>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.impl.RequiredFieldsValidator}</li>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.impl.TimestampValidator}</li>
 *   <li>{@link com.carbonfootprint.platform.ingestion.validation.impl.CategoryValidator}</li>
 * </ul>
 */
public interface ActivityValidator {

    /**
     * Validates the given activity.
     *
     * @param activity the activity to validate
     * @return a {@link ValidationResult} indicating pass or fail
     */
    ValidationResult validate(Activity activity);
}
