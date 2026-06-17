package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.MerchantIndustry;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Infers the merchant industry segment from a {@link CarbonHintContext}.
 *
 * <p>This provider runs at the lowest priority (order 60) so that more specific
 * providers (fuel, electricity, flight, transport) can claim the activity type
 * first. The merchant industry classification complements — never overrides — those.
 *
 * <h3>Industry segments detected</h3>
 * <ul>
 *   <li>GROCERY     — grocery chains, supermarkets, quick-commerce apps</li>
 *   <li>RESTAURANT  — food delivery, fast food, cafes, restaurants</li>
 *   <li>CLOTHING    — fashion retail, apparel brands</li>
 *   <li>ELECTRONICS — consumer electronics retailers</li>
 *   <li>PHARMACY    — pharmacies, chemists, health/medical stores</li>
 *   <li>FURNITURE   — furniture and home decor retailers</li>
 * </ul>
 *
 * <p>The internal keyword map is a {@link LinkedHashMap} to ensure deterministic
 * matching — more specific brands are placed before generic keywords.
 */
@Slf4j
@Component
public class MerchantIndustryHintProvider implements CarbonHintProvider {

    private static final int ORDER = 60;

    // LinkedHashMap — earlier entries take precedence for determinism
    private static final Map<String, MerchantIndustry> INDUSTRY_KEYWORDS = new LinkedHashMap<>();

    static {
        // ── GROCERY ────────────────────────────────────────────────────────
        INDUSTRY_KEYWORDS.put("bigbasket",       MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("big basket",      MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("grofers",         MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("blinkit",         MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("zepto",           MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("dmart",           MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("d mart",          MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("reliance fresh",  MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("more supermarket",MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("more retail",     MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("spencers",        MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("nature's basket", MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("star bazaar",     MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("lulu hypermarket",MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("hypermarket",     MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("supermarket",     MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("grocery",         MerchantIndustry.GROCERY);
        INDUSTRY_KEYWORDS.put("kirana",          MerchantIndustry.GROCERY);

        // ── RESTAURANT ─────────────────────────────────────────────────────
        INDUSTRY_KEYWORDS.put("zomato",          MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("swiggy",          MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("dunzo",           MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("mcdonalds",       MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("mcdonald's",      MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("kfc",             MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("pizza hut",       MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("dominos",         MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("domino's",        MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("burger king",     MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("starbucks",       MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("cafe coffee day", MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("barista",         MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("haldiram",        MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("barbeque nation", MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("restaurant",      MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("cafe",            MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("dhaba",           MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("hotel restaurant",MerchantIndustry.RESTAURANT);
        INDUSTRY_KEYWORDS.put("food delivery",   MerchantIndustry.RESTAURANT);

        // ── CLOTHING ───────────────────────────────────────────────────────
        INDUSTRY_KEYWORDS.put("myntra",          MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("ajio",            MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("zara",            MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("h&m",             MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("uniqlo",          MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("max fashion",     MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("westside",        MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("pantaloons",      MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("shoppers stop",   MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("lifestyle",       MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("fabindia",        MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("biba",            MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("clothing",        MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("apparel",         MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("garments",        MerchantIndustry.CLOTHING);
        INDUSTRY_KEYWORDS.put("fashion",         MerchantIndustry.CLOTHING);

        // ── ELECTRONICS ────────────────────────────────────────────────────
        INDUSTRY_KEYWORDS.put("croma",           MerchantIndustry.ELECTRONICS);
        INDUSTRY_KEYWORDS.put("vijay sales",     MerchantIndustry.ELECTRONICS);
        INDUSTRY_KEYWORDS.put("reliance digital",MerchantIndustry.ELECTRONICS);
        INDUSTRY_KEYWORDS.put("poorvika",        MerchantIndustry.ELECTRONICS);
        INDUSTRY_KEYWORDS.put("sangeetha",       MerchantIndustry.ELECTRONICS);
        INDUSTRY_KEYWORDS.put("apple store",     MerchantIndustry.ELECTRONICS);
        INDUSTRY_KEYWORDS.put("samsung store",   MerchantIndustry.ELECTRONICS);
        INDUSTRY_KEYWORDS.put("electronics",     MerchantIndustry.ELECTRONICS);
        INDUSTRY_KEYWORDS.put("mobile store",    MerchantIndustry.ELECTRONICS);
        INDUSTRY_KEYWORDS.put("laptop",          MerchantIndustry.ELECTRONICS);

        // ── PHARMACY ───────────────────────────────────────────────────────
        INDUSTRY_KEYWORDS.put("apollo pharmacy", MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("apollo health",   MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("medplus",         MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("1mg",             MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("netmeds",         MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("pharmeasy",       MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("wellness forever",MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("health & glow",   MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("pharmacy",        MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("chemist",         MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("medical store",   MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("medicine",        MerchantIndustry.PHARMACY);
        INDUSTRY_KEYWORDS.put("drugstore",       MerchantIndustry.PHARMACY);

        // ── FURNITURE ──────────────────────────────────────────────────────
        INDUSTRY_KEYWORDS.put("ikea",            MerchantIndustry.FURNITURE);
        INDUSTRY_KEYWORDS.put("urban ladder",    MerchantIndustry.FURNITURE);
        INDUSTRY_KEYWORDS.put("pepperfry",       MerchantIndustry.FURNITURE);
        INDUSTRY_KEYWORDS.put("wooden street",   MerchantIndustry.FURNITURE);
        INDUSTRY_KEYWORDS.put("nilkamal",        MerchantIndustry.FURNITURE);
        INDUSTRY_KEYWORDS.put("godrej interio",  MerchantIndustry.FURNITURE);
        INDUSTRY_KEYWORDS.put("furniture",       MerchantIndustry.FURNITURE);
        INDUSTRY_KEYWORDS.put("home decor",      MerchantIndustry.FURNITURE);
        INDUSTRY_KEYWORDS.put("home furnishing", MerchantIndustry.FURNITURE);
        INDUSTRY_KEYWORDS.put("mattress",        MerchantIndustry.FURNITURE);
    }

    @Override
    public CarbonHints provide(CarbonHintContext context) {
        MerchantIndustry industry = null;
        String matchedKeyword = null;
        boolean merchantMatch = false;

        for (Map.Entry<String, MerchantIndustry> entry : INDUSTRY_KEYWORDS.entrySet()) {
            if (context.contains(entry.getKey())) {
                industry = entry.getValue();
                matchedKeyword = entry.getKey();
                merchantMatch = context.merchantContains(entry.getKey());
                break;
            }
        }

        if (industry == null) {
            return CarbonHints.empty();
        }

        double confidence = merchantMatch ? CONFIDENCE_MERCHANT_MATCH : CONFIDENCE_KEYWORD_MATCH;

        log.debug("MerchantIndustryHintProvider — inferred: merchantIndustry={} keyword='{}' " +
                        "merchantMatch={} confidence={}",
                industry, matchedKeyword, merchantMatch, confidence);

        return CarbonHints.builder()
                .merchantIndustry(industry)
                .confidence(confidence)
                .build();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
