package com.carbonfootprint.platform.ingestion.normalization.impl;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.ingestion.normalization.ActivityNormalizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Normalizes physical quantity units to a canonical lowercase abbreviation.
 *
 * <p>OCR may produce unit strings in many formats. This normalizer maps
 * common variants to a single canonical form that downstream carbon
 * calculation engines can rely on.
 *
 * <p>Examples:
 * <ul>
 *   <li>"KWH", "Kwh", "kilowatt-hour" → "kWh"</li>
 *   <li>"L", "ltr", "Litre" → "litres"</li>
 *   <li>"KG", "Kilogram" → "kg"</li>
 *   <li>"KM", "Kilometre" → "km"</li>
 * </ul>
 *
 * <p>TODO (Phase 2): Load full mapping from Firestore config collection.
 */
@Component
@Order(4)
public class UnitNormalizer implements ActivityNormalizer {

    private static final Map<String, String> UNIT_MAP = Map.ofEntries(
            // Energy
            Map.entry("kwh",           "kWh"),
            Map.entry("kilowatt-hour", "kWh"),
            Map.entry("kilowatthour",  "kWh"),
            Map.entry("kw-h",          "kWh"),
            // Volume
            Map.entry("l",             "litres"),
            Map.entry("ltr",           "litres"),
            Map.entry("lts",           "litres"),
            Map.entry("litre",         "litres"),
            Map.entry("liter",         "litres"),
            Map.entry("liters",        "litres"),
            // Mass
            Map.entry("kg",            "kg"),
            Map.entry("kilogram",      "kg"),
            Map.entry("kilograms",     "kg"),
            Map.entry("g",             "g"),
            Map.entry("gram",          "g"),
            // Distance
            Map.entry("km",            "km"),
            Map.entry("kilometre",     "km"),
            Map.entry("kilometer",     "km"),
            Map.entry("miles",         "miles"),
            Map.entry("mile",          "miles")
    );

    @Override
    public Activity normalize(Activity activity) {
        if (!StringUtils.hasText(activity.getUnit())) {
            return activity;
        }

        String key = activity.getUnit().trim().toLowerCase();
        String normalised = UNIT_MAP.getOrDefault(key, activity.getUnit().trim());

        return activity.withUnit(normalised);
    }
}
