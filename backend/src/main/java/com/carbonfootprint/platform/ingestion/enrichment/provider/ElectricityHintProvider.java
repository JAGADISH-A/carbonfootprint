package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.EnergySource;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Infers electricity-specific carbon hints from a {@link CarbonHintContext}.
 *
 * <h3>Inference signals (in descending priority)</h3>
 * <ol>
 *   <li><strong>Merchant name</strong> — known Indian electricity utility brands</li>
 *   <li><strong>Unit field</strong> — "kWh", "units" (electricity bills often list unit consumed)</li>
 *   <li><strong>Description / items</strong> — electricity billing keywords</li>
 *   <li><strong>Category</strong> — {@link ActivityCategory#ELECTRICITY}</li>
 * </ol>
 *
 * <h3>Key inferences</h3>
 * <ul>
 *   <li>{@code electricityUnit} — always "KWH" when electricity is detected</li>
 *   <li>{@code energySource}    — {@link EnergySource#GRID} by default;
 *       {@link EnergySource#SOLAR} if solar keywords found</li>
 * </ul>
 */
@Slf4j
@Component
public class ElectricityHintProvider implements CarbonHintProvider {

    private static final int ORDER = 20;

    // ── Known electricity utility brand keywords ────────────────────────────

    private static final Set<String> ELECTRICITY_BRANDS = Set.of(
            "bescom", "msedcl", "tata power", "adani electricity", "adani power",
            "bses", "bses rajdhani", "bses yamuna",
            "tneb", "tangedco",
            "wbsedcl",
            "discoms", "discom",
            "cesc",
            "torrent power",
            "jdvvnl", "avvnl",
            "electricity board", "power supply", "electricity department",
            "state electricity",
            "kseb", "ksebl",
            "tsspdcl", "tsnpdcl",
            "apcpdcl", "apepdcl"
    );

    // ── Electricity keyword signals ─────────────────────────────────────────

    private static final Set<String> ELECTRICITY_KEYWORDS = Set.of(
            "kwh", "kw-h", "kilowatt", "kilowatt-hour",
            "electricity", "electric bill", "electricity bill",
            "power bill", "energy bill", "electricity consumption",
            "units consumed", "units of electricity",
            "meter reading", "meter no", "bill no", "tariff",
            "eb bill", "eb payment"
    );

    // ── Solar / renewable signals ───────────────────────────────────────────

    private static final Set<String> SOLAR_KEYWORDS = Set.of(
            "solar", "solar power", "solar energy", "solar panel",
            "rooftop solar", "net metering"
    );

    @Override
    public CarbonHints provide(CarbonHintContext context) {
        // ── 1: Merchant-brand signal ───────────────────────────────────────
        boolean brandMatched = context.anyMatch(ELECTRICITY_BRANDS);

        // ── 2: Keyword signal in description/items ─────────────────────────
        boolean keywordMatched = !brandMatched && context.anyMatch(ELECTRICITY_KEYWORDS);

        // ── 3: Unit field signal ───────────────────────────────────────────
        boolean unitSignal = !context.getUnit().isEmpty()
                && (context.getUnit().contains("kwh") || context.getUnit().equals("units"));

        // ── 4: Category signal ─────────────────────────────────────────────
        boolean categoryElectricity = context.hasCategory(ActivityCategory.ELECTRICITY);

        if (!brandMatched && !keywordMatched && !unitSignal && !categoryElectricity) {
            return CarbonHints.empty();
        }

        // ── 5: Detect energy source ────────────────────────────────────────
        EnergySource energySource = context.anyMatch(SOLAR_KEYWORDS)
                ? EnergySource.SOLAR : EnergySource.GRID;

        // ── 6: Compute confidence ──────────────────────────────────────────
        double confidence;
        if (brandMatched) {
            confidence = CONFIDENCE_MERCHANT_MATCH;
        } else if (unitSignal) {
            confidence = CONFIDENCE_UNIT_SIGNAL;
        } else if (keywordMatched) {
            confidence = CONFIDENCE_KEYWORD_MATCH;
        } else {
            confidence = CONFIDENCE_CATEGORY_ONLY;
        }

        log.debug("ElectricityHintProvider — inferred: energySource={} confidence={}",
                energySource, confidence);

        return CarbonHints.builder()
                .activityType(CarbonActivityType.ELECTRICITY)
                .electricityUnit("KWH")
                .energySource(energySource)
                .confidence(confidence)
                .build();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
