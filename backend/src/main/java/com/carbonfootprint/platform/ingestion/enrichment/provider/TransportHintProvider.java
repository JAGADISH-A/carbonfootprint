package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Infers ground-transport carbon hints from a {@link CarbonHintContext}.
 *
 * <h3>Transport mode detection (in precedence order)</h3>
 * <ul>
 *   <li>BUS       — "bus", "ksrtc", "bmtc", "msrtc", "upsrtc", "redbus", "volvo bus"</li>
 *   <li>METRO     — "metro", "dmrc", "bmrc", "nmmc metro", "hyderabad metro", "chennai metro"</li>
 *   <li>TRAIN     — "train", "irctc", "indian rail", "indian railways", "rail ticket"</li>
 *   <li>TAXI      — "uber", "ola", "rapido", "meru", "taxi", "cab", "yellow cab"</li>
 *   <li>AUTO      — "auto", "autorickshaw", "auto rickshaw", "tuk tuk"</li>
 *   <li>BIKE      — "bike", "bicycle", "cycle", "yulu", "bounce"</li>
 * </ul>
 *
 * <p>The map is {@link LinkedHashMap} so earlier entries take precedence (train before taxi, etc.).
 */
@Slf4j
@Component
public class TransportHintProvider implements CarbonHintProvider {

    private static final int ORDER = 40;

    // LinkedHashMap — iteration order is declaration order; earlier entries win
    private static final Map<String, TransportMode> MODE_KEYWORDS = new LinkedHashMap<>();

    static {
        // BUS (public transit)
        MODE_KEYWORDS.put("ksrtc",            TransportMode.BUS);
        MODE_KEYWORDS.put("bmtc",             TransportMode.BUS);
        MODE_KEYWORDS.put("msrtc",            TransportMode.BUS);
        MODE_KEYWORDS.put("upsrtc",           TransportMode.BUS);
        MODE_KEYWORDS.put("gsrtc",            TransportMode.BUS);
        MODE_KEYWORDS.put("redbus",           TransportMode.BUS);
        MODE_KEYWORDS.put("volvo bus",        TransportMode.BUS);
        MODE_KEYWORDS.put("city bus",         TransportMode.BUS);
        // METRO
        MODE_KEYWORDS.put("dmrc",             TransportMode.METRO);
        MODE_KEYWORDS.put("bmrc",             TransportMode.METRO);
        MODE_KEYWORDS.put("nmmc metro",       TransportMode.METRO);
        MODE_KEYWORDS.put("hyderabad metro",  TransportMode.METRO);
        MODE_KEYWORDS.put("chennai metro",    TransportMode.METRO);
        MODE_KEYWORDS.put("kochi metro",      TransportMode.METRO);
        MODE_KEYWORDS.put("pune metro",       TransportMode.METRO);
        MODE_KEYWORDS.put("metro rail",       TransportMode.METRO);
        MODE_KEYWORDS.put("metro card",       TransportMode.METRO);
        // TRAIN
        MODE_KEYWORDS.put("irctc",            TransportMode.TRAIN);
        MODE_KEYWORDS.put("indian railways",  TransportMode.TRAIN);
        MODE_KEYWORDS.put("indian rail",      TransportMode.TRAIN);
        MODE_KEYWORDS.put("rail ticket",      TransportMode.TRAIN);
        MODE_KEYWORDS.put("railway",          TransportMode.TRAIN);
        // TAXI / rideshare
        MODE_KEYWORDS.put("uber",             TransportMode.TAXI);
        MODE_KEYWORDS.put("ola cabs",         TransportMode.TAXI);
        MODE_KEYWORDS.put("rapido",           TransportMode.TAXI);
        MODE_KEYWORDS.put("meru",             TransportMode.TAXI);
        MODE_KEYWORDS.put("yellow cab",       TransportMode.TAXI);
        MODE_KEYWORDS.put("cab",              TransportMode.TAXI);
        // AUTO
        MODE_KEYWORDS.put("autorickshaw",     TransportMode.AUTO);
        MODE_KEYWORDS.put("auto rickshaw",    TransportMode.AUTO);
        MODE_KEYWORDS.put("tuk tuk",          TransportMode.AUTO);
        // BIKE / cycle
        MODE_KEYWORDS.put("yulu",             TransportMode.BIKE);
        MODE_KEYWORDS.put("bounce",           TransportMode.BIKE);
        MODE_KEYWORDS.put("bicycle",          TransportMode.BIKE);
        MODE_KEYWORDS.put("cycle",            TransportMode.BIKE);
        // Generic multi-word keywords last (lower specificity)
        MODE_KEYWORDS.put("metro",            TransportMode.METRO);
        MODE_KEYWORDS.put("train",            TransportMode.TRAIN);
        MODE_KEYWORDS.put("taxi",             TransportMode.TAXI);
        MODE_KEYWORDS.put("auto",             TransportMode.AUTO);
        MODE_KEYWORDS.put("bike",             TransportMode.BIKE);
        MODE_KEYWORDS.put("bus",              TransportMode.BUS);
        MODE_KEYWORDS.put("ola",              TransportMode.TAXI);
    }

    @Override
    public CarbonHints provide(CarbonHintContext context) {
        // Skip if category is definitively not transport (and not null)
        if (!context.hasCategoryOrNull(ActivityCategory.TRANSPORT)) {
            return CarbonHints.empty();
        }

        TransportMode transportMode = null;
        String matchedKeyword = null;
        boolean merchantMatch = false;

        for (Map.Entry<String, TransportMode> entry : MODE_KEYWORDS.entrySet()) {
            if (context.contains(entry.getKey())) {
                transportMode = entry.getValue();
                matchedKeyword = entry.getKey();
                // Determine if match was in merchant specifically
                merchantMatch = context.merchantContains(entry.getKey());
                break;
            }
        }

        if (transportMode == null) {
            // Category-only inference: TRANSPORT category → unknown mode
            if (context.hasCategory(ActivityCategory.TRANSPORT)) {
                log.debug("TransportHintProvider — inferred from category only: mode=UNKNOWN confidence={}",
                        CONFIDENCE_CATEGORY_ONLY);
                return CarbonHints.builder()
                        .activityType(CarbonActivityType.TRANSPORT)
                        .confidence(CONFIDENCE_CATEGORY_ONLY)
                        .build();
            }
            return CarbonHints.empty();
        }

        double confidence = merchantMatch ? CONFIDENCE_MERCHANT_MATCH : CONFIDENCE_KEYWORD_MATCH;

        log.debug("TransportHintProvider — inferred: transportMode={} keyword='{}' merchantMatch={} confidence={}",
                transportMode, matchedKeyword, merchantMatch, confidence);

        return CarbonHints.builder()
                .activityType(CarbonActivityType.TRANSPORT)
                .transportMode(transportMode)
                .confidence(confidence)
                .build();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
