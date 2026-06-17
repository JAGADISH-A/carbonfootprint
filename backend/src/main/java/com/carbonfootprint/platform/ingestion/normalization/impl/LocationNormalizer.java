package com.carbonfootprint.platform.ingestion.normalization.impl;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.ingestion.normalization.ActivityNormalizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Normalizes location strings to a consistent format.
 *
 * <p>Location data from OCR, email, or manual input may arrive in various
 * formats: full addresses, city names, ISO country codes, coordinates.
 * This normalizer applies lightweight cleanup.
 *
 * <p>Examples:
 * <ul>
 *   <li>"  bangalore, india  " → "Bangalore, India"</li>
 *   <li>"MUMBAI" → "Mumbai"</li>
 * </ul>
 *
 * <p>TODO (Phase 2): Integrate with a geocoding API (e.g., Google Maps
 * Geocoding API) to resolve free-text locations to ISO 3166-1 country codes
 * and GeoJSON coordinates. Store in {@code Activity.metadata}.
 */
@Component
@Order(5)
public class LocationNormalizer implements ActivityNormalizer {

    @Override
    public Activity normalize(Activity activity) {
        if (!StringUtils.hasText(activity.getLocation())) {
            return activity;
        }

        // TODO (Phase 2): Geocoding integration
        String normalized = toTitleCase(activity.getLocation().trim());

        return activity.withLocation(normalized);
    }

    private String toTitleCase(String input) {
        String[] parts = input.split(",");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                String[] words = trimmed.toLowerCase().split("\\s+");
                for (String word : words) {
                    if (!word.isEmpty()) {
                        sb.append(Character.toUpperCase(word.charAt(0)))
                          .append(word.substring(1))
                          .append(" ");
                    }
                }
                // Remove trailing space, add comma separator
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                    sb.append(", ");
                }
            }
        }
        if (sb.length() >= 2) {
            sb.setLength(sb.length() - 2); // Remove trailing ", "
        }
        return sb.toString();
    }
}
