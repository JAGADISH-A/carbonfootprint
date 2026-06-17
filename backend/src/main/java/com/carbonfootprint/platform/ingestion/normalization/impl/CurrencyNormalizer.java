package com.carbonfootprint.platform.ingestion.normalization.impl;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.ingestion.normalization.ActivityNormalizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * Normalizes currency codes to ISO 4217 uppercase 3-letter format.
 *
 * <p>OCR and email parsers may return currency in various formats.
 * This normalizer maps common variants to the canonical ISO code.
 *
 * <p>Examples:
 * <ul>
 *   <li>"Rs", "rs.", "INR" → "INR"</li>
 *   <li>"$", "USD", "usd" → "USD"</li>
 *   <li>"€", "EUR" → "EUR"</li>
 *   <li>"£", "GBP" → "GBP"</li>
 * </ul>
 *
 * <p>TODO (Phase 2): Load the full mapping from a Firestore config collection.
 */
@Component
@Order(2)
public class CurrencyNormalizer implements ActivityNormalizer {

    /**
     * Map of raw currency strings (lowercase) to ISO 4217 codes.
     * Future: load from Firestore configuration collection.
     */
    private static final Map<String, String> CURRENCY_MAP = Map.ofEntries(
            Map.entry("rs",  "INR"),
            Map.entry("rs.", "INR"),
            Map.entry("inr", "INR"),
            Map.entry("₹",   "INR"),
            Map.entry("$",   "USD"),
            Map.entry("usd", "USD"),
            Map.entry("€",   "EUR"),
            Map.entry("eur", "EUR"),
            Map.entry("£",   "GBP"),
            Map.entry("gbp", "GBP"),
            Map.entry("¥",   "JPY"),
            Map.entry("jpy", "JPY"),
            Map.entry("sgd", "SGD"),
            Map.entry("aud", "AUD"),
            Map.entry("cad", "CAD")
    );

    @Override
    public Activity normalize(Activity activity) {
        if (!StringUtils.hasText(activity.getCurrency())) {
            return activity;
        }

        String normalised = CURRENCY_MAP.getOrDefault(
                activity.getCurrency().trim().toLowerCase(),
                activity.getCurrency().trim().toUpperCase()
        );

        return activity.withCurrency(normalised);
    }
}
