package com.carbonfootprint.platform.ingestion.normalization;

import com.carbonfootprint.platform.activity.model.Activity;

/**
 * Normalizer for {@link Activity} domain objects.
 *
 * <p>Normalizers form an ordered chain injected as {@code List<ActivityNormalizer>}
 * into the pipeline service. Each normalizer transforms one aspect of the
 * activity and returns a new (immutable) instance via Lombok {@code @With}.
 *
 * <h3>Single Responsibility</h3>
 * Each implementation handles exactly one normalisation concern:
 * <ul>
 *   <li>Merchant names → {@link com.carbonfootprint.platform.ingestion.normalization.impl.MerchantNormalizer}</li>
 *   <li>Currency codes → {@link com.carbonfootprint.platform.ingestion.normalization.impl.CurrencyNormalizer}</li>
 *   <li>Timestamps → {@link com.carbonfootprint.platform.ingestion.normalization.impl.DateNormalizer}</li>
 *   <li>Physical units → {@link com.carbonfootprint.platform.ingestion.normalization.impl.UnitNormalizer}</li>
 *   <li>Location strings → {@link com.carbonfootprint.platform.ingestion.normalization.impl.LocationNormalizer}</li>
 * </ul>
 *
 * <h3>Immutability</h3>
 * Implementations must NOT mutate the input activity. They must return a
 * new instance produced via {@code activity.with*()} (Lombok {@code @With}).
 */
public interface ActivityNormalizer {

    /**
     * Applies a single normalisation transformation to the activity.
     *
     * @param activity the activity to normalise (must not be mutated)
     * @return a new {@link Activity} instance with the normalisation applied
     */
    Activity normalize(Activity activity);
}
