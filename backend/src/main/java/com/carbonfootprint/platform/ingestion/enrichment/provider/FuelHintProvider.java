package com.carbonfootprint.platform.ingestion.enrichment.provider;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintContext;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintProvider;
import com.carbonfootprint.platform.ingestion.enrichment.model.CarbonActivityType;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.VehicleType;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Infers fuel-specific carbon hints from a {@link CarbonHintContext}.
 *
 * <h3>Inference signals (in descending priority)</h3>
 * <ol>
 *   <li><strong>Merchant name</strong> — known Indian/global fuel station brands</li>
 *   <li><strong>Description + items</strong> — explicit fuel-type keywords</li>
 *   <li><strong>Category</strong> — {@link ActivityCategory#FUEL} with {@code fuelType=UNKNOWN}</li>
 * </ol>
 *
 * <h3>Fuel-type detection</h3>
 * <ul>
 *   <li>PETROL  — keyword "petrol", "gasoline"</li>
 *   <li>DIESEL  — keyword "diesel", "hsd" (High Speed Diesel)</li>
 *   <li>LPG     — keyword "lpg", "autogas"</li>
 *   <li>CNG     — keyword "cng", "compressed natural gas"</li>
 *   <li>UNKNOWN — station brand matched but no specific fuel keyword found</li>
 * </ul>
 */
@Slf4j
@Component
public class FuelHintProvider implements CarbonHintProvider {

    private static final int ORDER = 10;

    // ── Known fuel station brand keywords ──────────────────────────────────

    private static final Set<String> FUEL_STATION_BRANDS = Set.of(
            "indianoil", "indian oil", "iocl",
            "hp", "hpcl", "hindustan petroleum",
            "bpcl", "bharat petroleum",
            "shell",
            "reliance petroleum",
            "essar", "nayara",
            "total", "totalenergies",
            "caltex",
            "petro",
            "fuel", "petrol pump", "filling station", "gas station", "service station"
    );

    // ── Fuel-type keyword clusters (LinkedHashMap for deterministic order) ──

    private static final Map<String, FuelType> FUEL_TYPE_KEYWORDS = new LinkedHashMap<>();

    static {
        FUEL_TYPE_KEYWORDS.put("petrol",                 FuelType.PETROL);
        FUEL_TYPE_KEYWORDS.put("gasoline",               FuelType.PETROL);
        FUEL_TYPE_KEYWORDS.put("diesel",                 FuelType.DIESEL);
        FUEL_TYPE_KEYWORDS.put("hsd",                    FuelType.DIESEL);
        FUEL_TYPE_KEYWORDS.put("high speed diesel",      FuelType.DIESEL);
        FUEL_TYPE_KEYWORDS.put("lpg",                    FuelType.LPG);
        FUEL_TYPE_KEYWORDS.put("autogas",                FuelType.LPG);
        FUEL_TYPE_KEYWORDS.put("cng",                    FuelType.CNG);
        FUEL_TYPE_KEYWORDS.put("compressed natural gas", FuelType.CNG);
    }

    // ── Vehicle type keywords (LinkedHashMap for deterministic order) ───────

    private static final Map<String, VehicleType> VEHICLE_KEYWORDS = new LinkedHashMap<>();

    static {
        VEHICLE_KEYWORDS.put("car",        VehicleType.CAR);
        VEHICLE_KEYWORDS.put("bike",       VehicleType.MOTORBIKE);
        VEHICLE_KEYWORDS.put("motorbike",  VehicleType.MOTORBIKE);
        VEHICLE_KEYWORDS.put("motorcycle", VehicleType.MOTORBIKE);
        VEHICLE_KEYWORDS.put("scooter",    VehicleType.MOTORBIKE);
        VEHICLE_KEYWORDS.put("truck",      VehicleType.TRUCK);
        VEHICLE_KEYWORDS.put("lorry",      VehicleType.TRUCK);
        VEHICLE_KEYWORDS.put("auto",       VehicleType.AUTO_RICKSHAW);
    }

    @Override
    public CarbonHints provide(CarbonHintContext context) {
        // ── 1: Merchant-brand signal ───────────────────────────────────────
        boolean brandMatched = context.anyMatch(FUEL_STATION_BRANDS);

        // ── 2: Category signal ─────────────────────────────────────────────
        boolean categoryFuel = context.hasCategory(ActivityCategory.FUEL);

        if (!brandMatched && !categoryFuel) {
            // Check if any fuel-type keyword appears without a brand signal
            if (!context.anyMatch(FUEL_TYPE_KEYWORDS.keySet())) {
                return CarbonHints.empty();
            }
        }

        // ── 3: Detect specific fuel type ───────────────────────────────────
        FuelType fuelType = context.findFirstMatch(FUEL_TYPE_KEYWORDS)
                .map(Map.Entry::getValue)
                .orElse(FuelType.UNKNOWN);

        // ── 4: Detect vehicle type ─────────────────────────────────────────
        VehicleType vehicleType = context.findFirstMatch(VEHICLE_KEYWORDS)
                .map(Map.Entry::getValue)
                .orElse(null);

        // ── 5: Compute confidence ──────────────────────────────────────────
        double confidence = brandMatched ? CONFIDENCE_MERCHANT_MATCH
                : categoryFuel ? CONFIDENCE_CATEGORY_ONLY
                : CONFIDENCE_KEYWORD_MATCH;

        log.debug("FuelHintProvider — inferred: fuelType={} vehicleType={} confidence={}",
                fuelType, vehicleType, confidence);

        return CarbonHints.builder()
                .activityType(CarbonActivityType.FUEL)
                .fuelType(fuelType)
                .fuelUnit("LITRE")
                .vehicleType(vehicleType)
                .confidence(confidence)
                .build();
    }

    @Override
    public int getOrder() {
        return ORDER;
    }
}
