package com.carbonfootprint.platform.carbon.calculation.port.out;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;

import java.util.List;

/**
 * Outbound port: persistence contract for {@link EmissionFactor} data.
 *
 * <h3>Clean Architecture role</h3>
 * This interface is a <em>driven port</em> — the registry adapter depends on
 * this abstraction to load emission factors from the data source (Firestore,
 * YAML, database, etc.).
 *
 * <h3>Implementations</h3>
 * <ul>
 *   <li>{@link com.carbonfootprint.platform.carbon.calculation.adapter.out.firestore.FirestoreEmissionFactorRepository}</li>
 * </ul>
 */
public interface EmissionFactorRepository {

    /**
     * Retrieves all emission factors from the data source.
     *
     * @return list of all emission factors (may be empty)
     */
    List<EmissionFactor> findAll();

    /**
     * Retrieves all emission factors matching the given category.
     *
     * @param category the activity category to filter by
     * @return list of matching factors (may be empty)
     */
    List<EmissionFactor> findByCategory(ActivityCategory category);
}
