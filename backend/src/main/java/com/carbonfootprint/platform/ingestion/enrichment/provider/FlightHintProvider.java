package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.CabinClass;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Infers flight-specific carbon hints from a {@link CarbonHintContext}.
 *
 * <h3>Inference signals</h3>
 * <ol>
 *   <li><strong>Merchant name</strong> — known Indian and international airline brands</li>
 *   <li><strong>Description / items</strong> — cabin class, passenger count keywords</li>
 *   <li><strong>Category</strong> — {@link ActivityCategory#FLIGHT}</li>
 * </ol>
 *
 * <h3>Cabin class detection</h3>
 * <ul>
 *   <li>ECONOMY         — "economy", "eco"</li>
 *   <li>PREMIUM_ECONOMY — "premium economy", "premium eco"</li>
 *   <li>BUSINESS        — "business", "biz class", "club class"</li>
 *   <li>FIRST           — "first class", "first"</li>
 * </ul>
 *
 * <h3>Passenger count detection</h3>
 * Regex patterns like "2 passengers", "3 pax", "for 1 adult", "adults: 2".
 */
@Slf4j
@Component
public class FlightHintProvider implements CarbonHintProvider {

    private static final int ORDER = 30;

    // ── Known airline brand keywords ────────────────────────────────────────

    private static final Set<String> AIRLINE_BRANDS = Set.of(
            // Indian carriers
            "indigo", "air india", "vistara", "spicejet",
            "goair", "go first", "go airlines", "akasa", "akasa air",
            "alliance air", "air asia india", "airasia",
            // International majors
            "emirates", "lufthansa", "british airways", "qantas",
            "united airlines", "delta", "american airlines",
            "singapore airlines", "cathay pacific",
            "thai airways", "malaysia airlines",
            "air france", "klm", "turkish airlines",
            "etihad", "qatar airways", "flydubai",
            // Generic signals
            "airlines", "air ticket", "flight ticket", "boarding pass",
            "aviation", "airport tax"
    );

    // ── Cabin class keywords (LinkedHashMap: more specific first) ──────────

    private static final Map<String, CabinClass> CABIN_KEYWORDS = new LinkedHashMap<>();

    static {
        CABIN_KEYWORDS.put("premium economy", CabinClass.PREMIUM_ECONOMY);
        CABIN_KEYWORDS.put("premium eco",     CabinClass.PREMIUM_ECONOMY);
        CABIN_KEYWORDS.put("business class",  CabinClass.BUSINESS);
        CABIN_KEYWORDS.put("biz class",       CabinClass.BUSINESS);
        CABIN_KEYWORDS.put("club class",      CabinClass.BUSINESS);
        CABIN_KEYWORDS.put("first class",     CabinClass.FIRST);
        CABIN_KEYWORDS.put("economy class",   CabinClass.ECONOMY);
        CABIN_KEYWORDS.put("economy",         CabinClass.ECONOMY);
        CABIN_KEYWORDS.put("eco class",       CabinClass.ECONOMY);
    }

    // Specific "first" keyword — handled separately to avoid false positives
    private static final Pattern FIRST_PATTERN =
            Pattern.compile("\\bfirst\\b(?!\\s+name|\\s+floor|\\s+time)", Pattern.CASE_INSENSITIVE);

    // ── Passenger count patterns ────────────────────────────────────────────

    private static final List<Pattern> PASSENGER_PATTERNS = List.of(
            Pattern.compile("(\\d+)\\s*(?:passenger|passengers|pax)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+)\\s*(?:adult|adults)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("adults?\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("passengers?\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("for\\s+(\\d+)\\s+(?:adult|person|travell?er)", Pattern.CASE_INSENSITIVE)
    );

    @Override
    public CarbonHints provide(CarbonHintContext context) {
        // ── 1: Merchant-brand signal (confidence 0.9) ──────────────────────
        boolean brandMatched = context.anyMatch(AIRLINE_BRANDS);

        // ── 2: Category signal (confidence 0.5) ───────────────────────────
        boolean categoryFlight = context.hasCategory(ActivityCategory.FLIGHT);

        if (!brandMatched && !categoryFlight) {
            return CarbonHints.empty();
        }

        // ── 3: Detect cabin class ──────────────────────────────────────────
        CabinClass cabinClass = detectCabinClass(context.getCorpus());

        // ── 4: Detect passenger count ──────────────────────────────────────
        Integer passengerCount = detectPassengerCount(context.getCorpus());

        // ── 5: Compute confidence ──────────────────────────────────────────
        double confidence = brandMatched ? CONFIDENCE_MERCHANT_MATCH : CONFIDENCE_CATEGORY_ONLY;

        log.debug("FlightHintProvider — inferred: cabinClass={} passengerCount={} confidence={}",
                cabinClass, passengerCount, confidence);

        return CarbonHints.builder()
                .activityType(CarbonActivityType.FLIGHT)
                .cabinClass(cabinClass)
                .passengerCount(passengerCount)
                .confidence(confidence)
                .build();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    // ── Detection helpers ────────────────────────────────────────────────────

    private CabinClass detectCabinClass(String corpus) {
        // Check ordered cabin keywords (more specific first)
        for (Map.Entry<String, CabinClass> entry : CABIN_KEYWORDS.entrySet()) {
            if (corpus.contains(entry.getKey())) {
                log.debug("FlightHintProvider — cabin class keyword matched: '{}'", entry.getKey());
                return entry.getValue();
            }
        }

        // Check standalone "first" with negative lookahead (use original corpus for regex)
        if (FIRST_PATTERN.matcher(corpus).find()) {
            log.debug("FlightHintProvider — cabin class 'first' matched via pattern");
            return CabinClass.FIRST;
        }

        // Default to ECONOMY if flight is detected but cabin not specified
        log.debug("FlightHintProvider — no cabin class keyword found, defaulting to ECONOMY");
        return CabinClass.ECONOMY;
    }

    private Integer detectPassengerCount(String corpus) {
        for (Pattern pattern : PASSENGER_PATTERNS) {
            Matcher matcher = pattern.matcher(corpus);
            if (matcher.find()) {
                try {
                    int count = Integer.parseInt(matcher.group(1));
                    if (count > 0 && count <= 9) { // sanity bound
                        log.debug("FlightHintProvider — passenger count matched: {}", count);
                        return count;
                    }
                } catch (NumberFormatException ignored) {
                    // regex guarantees digits, but guard anyway
                }
            }
        }
        return null;
    }
}
