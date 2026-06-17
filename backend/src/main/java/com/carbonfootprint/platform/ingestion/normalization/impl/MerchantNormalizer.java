package com.carbonfootprint.platform.ingestion.normalization.impl;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.ingestion.normalization.ActivityNormalizer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Normalizes merchant names to a canonical form.
 *
 * <p>Raw merchant names from OCR or email parsing often contain noise:
 * uppercase, trailing store codes, punctuation. This normalizer applies
 * lightweight cleanup so merchant names are consistent across sources.
 *
 * <p>Examples:
 * <ul>
 *   <li>"SHELL PETROL STN #1234" → "Shell"</li>
 *   <li>"STARBUCKS COFFEE 0045" → "Starbucks"</li>
 * </ul>
 *
 * <p>TODO (Phase 2): Load merchant name mappings from Firestore config
 * collection for fuzzy matching and brand canonicalization.
 */
@Component
@Order(1)
public class MerchantNormalizer implements ActivityNormalizer {

    @Override
    public Activity normalize(Activity activity) {
        if (!StringUtils.hasText(activity.getMerchant())) {
            return activity;
        }

        // TODO (Phase 2): Apply configurable merchant alias map from Firestore
        // For now: trim whitespace and apply basic title-case
        String normalized = toTitleCase(activity.getMerchant().trim());

        return activity.withMerchant(normalized);
    }

    private String toTitleCase(String input) {
        if (!StringUtils.hasText(input)) return input;
        String[] words = input.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                  .append(word.substring(1))
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }
}
