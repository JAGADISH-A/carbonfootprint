package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Infers the shopping activity type from a {@link CarbonHintContext}.
 *
 * <p>This provider focuses on confirming that the activity is SHOPPING (as opposed to
 * classifying the industry — that is handled by
 * {@link MerchantIndustryHintProvider}). It detects e-commerce platforms,
 * online marketplaces, and general retail signals.
 *
 * <h3>Inference signals</h3>
 * <ul>
 *   <li><strong>Merchant name</strong> — known e-commerce and retail brands</li>
 *   <li><strong>Description / items</strong> — shopping-related keywords</li>
 *   <li><strong>Category</strong> — {@link ActivityCategory#SHOPPING}</li>
 * </ul>
 */
@Slf4j
@Component
public class ShoppingHintProvider implements CarbonHintProvider {

    private static final int ORDER = 50;

    // ── Known e-commerce / retail brand signals ─────────────────────────────

    private static final Set<String> SHOPPING_BRANDS = Set.of(
            // Indian e-commerce
            "flipkart", "amazon", "amazon india",
            "myntra", "ajio", "nykaa", "meesho",
            "snapdeal", "shopclues",
            "tatacliq", "tata cliq",
            "jiomart",
            // Global
            "ebay", "etsy", "aliexpress", "shein",
            // Retail chains (not food-specific)
            "reliance digital", "croma", "vijay sales",
            "westside", "shoppers stop", "lifestyle",
            "pantaloons", "max fashion",
            "big bazaar",
            "ikea"
    );

    // ── Generic shopping keyword signals ────────────────────────────────────

    private static final Set<String> SHOPPING_KEYWORDS = Set.of(
            "online shopping", "e-commerce", "ecommerce",
            "order #", "order id", "order no",
            "invoice #", "invoice no",
            "purchase", "retail",
            "delivery charges", "shipping", "cod"
    );

    @Override
    public CarbonHints provide(CarbonHintContext context) {
        // Skip if category is definitively non-shopping
        if (!context.hasCategoryOrNull(ActivityCategory.SHOPPING)) {
            return CarbonHints.empty();
        }

        // ── 1: Brand signal (confidence 0.9) ───────────────────────────────
        boolean brandMatched = context.anyMatch(SHOPPING_BRANDS);

        // ── 2: Keyword signal (confidence 0.7) ─────────────────────────────
        boolean keywordMatched = !brandMatched && context.anyMatch(SHOPPING_KEYWORDS);

        // ── 3: Category signal (confidence 0.5) ───────────────────────────
        boolean categoryShopping = context.hasCategory(ActivityCategory.SHOPPING);

        if (!brandMatched && !keywordMatched && !categoryShopping) {
            return CarbonHints.empty();
        }

        double confidence = brandMatched ? CONFIDENCE_MERCHANT_MATCH
                : keywordMatched ? CONFIDENCE_KEYWORD_MATCH
                : CONFIDENCE_CATEGORY_ONLY;

        log.debug("ShoppingHintProvider — inferred: confidence={}", confidence);

        return CarbonHints.builder()
                .activityType(CarbonActivityType.SHOPPING)
                .confidence(confidence)
                .build();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
