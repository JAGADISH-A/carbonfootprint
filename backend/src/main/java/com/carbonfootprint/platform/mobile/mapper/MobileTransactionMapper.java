package com.carbonfootprint.platform.mobile.mapper;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.mobile.dto.EnrichedTransaction;
import com.carbonfootprint.platform.shared.util.IdGenerator;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps the {@link EnrichedTransaction} DTO from the Android Companion App into two
 * forms used by the downstream pipeline:
 *
 * <ol>
 *   <li>{@link ExtractionResult} — fed into the {@link com.carbonfootprint.platform.ingestion.enrichment.CarbonHintEngine}
 *       to validate and complete any missing carbon hints sent by the app.</li>
 *   <li>{@link Activity} — the final domain entity persisted to Firestore, built using
 *       the engine-verified {@link CarbonHints}.</li>
 * </ol>
 *
 * <p>The mapper explicitly sets {@link ActivitySource#MOBILE} on every produced Activity.
 * It never sets {@code rawDocumentId} — mobile transactions have no binary document.
 */
@Component
public class MobileTransactionMapper {

    private static final String CARBON_HINTS_KEY = "carbonHints";

    // ── ExtractionResult ───────────────────────────────────────────────────

    /**
     * Converts an {@link EnrichedTransaction} into an {@link ExtractionResult} suitable
     * for being fed into the {@link com.carbonfootprint.platform.ingestion.enrichment.CarbonHintEngine}.
     *
     * <p>The carbon hints already provided by the Android ML model are embedded in
     * {@code metadata.carbonHints} — the engine reads and uses them as the seed,
     * then supplements any missing fields with its own provider chain.
     */
    public ExtractionResult toExtractionResult(EnrichedTransaction transaction) {
        Map<String, Object> metadata = new HashMap<>();

        // Embed app-side hints as seed so the engine treats them with highest priority
        if (transaction.getCarbonHints() != null && !transaction.getCarbonHints().isEmpty()) {
            metadata.put(CARBON_HINTS_KEY, transaction.getCarbonHints());
        }

        // Carry generic metadata through
        if (transaction.getMetadata() != null) {
            transaction.getMetadata().forEach(metadata::putIfAbsent);
        }

        return ExtractionResult.builder()
                .merchant(transaction.getMerchant())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .category(transaction.getCategory() != null ? transaction.getCategory() : ActivityCategory.OTHER)
                .unit(transaction.getUnit())
                .location(transaction.getLocation())
                .occurredAt(transaction.getOccurredAt())
                .description(transaction.getDescription())
                // Mobile app already did structured extraction — use max confidence as seed.
                // The engine will recalculate confidence from provider outputs.
                .confidence(0.95)
                .parserName("MobileAndroidApp")
                .metadata(metadata)
                .build();
    }

    // ── Activity ───────────────────────────────────────────────────────────

    /**
     * Converts an {@link EnrichedTransaction} together with the backend-verified
     * {@link CarbonHints} (as produced by the {@code CarbonHintEngine}) into the
     * final {@link Activity} domain entity.
     *
     * @param transaction the raw mobile transaction DTO
     * @param finalHints  the backend-verified, merged carbon hints
     * @param userId      the authenticated user identifier
     * @param deviceId    the originating device identifier (stored for dedup)
     * @return a fully populated, ready-to-persist {@link Activity}
     */
    public Activity toActivity(EnrichedTransaction transaction,
                               CarbonHints finalHints,
                               String userId,
                               String deviceId) {

        Map<String, Object> metadata = buildMetadata(transaction, finalHints, deviceId);

        return Activity.builder()
                .id(IdGenerator.generate())
                .userId(userId)
                .source(ActivitySource.MOBILE)
                .category(transaction.getCategory() != null ? transaction.getCategory() : ActivityCategory.OTHER)
                .merchant(transaction.getMerchant())
                .amount(transaction.getAmount())
                .currency(transaction.getCurrency())
                .unit(transaction.getUnit())
                .location(transaction.getLocation())
                .occurredAt(transaction.getOccurredAt() != null ? transaction.getOccurredAt() : Instant.now())
                .description(transaction.getDescription())
                .rawDocumentId(null) // Mobile transactions have no binary document
                .metadata(metadata)
                .createdAt(Instant.now())
                .build();
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /**
     * Builds the metadata map that will be stored alongside the Activity in Firestore.
     *
     * <p>Includes:
     * <ul>
     *   <li>Backend-verified carbon hints</li>
     *   <li>Source tracing fields (deviceId, transactionId, confidence)</li>
     *   <li>Original app-side metadata</li>
     * </ul>
     */
    private Map<String, Object> buildMetadata(EnrichedTransaction transaction,
                                               CarbonHints finalHints,
                                               String deviceId) {
        Map<String, Object> metadata = new HashMap<>();

        // Carry through any app-supplied generic metadata
        if (transaction.getMetadata() != null) {
            metadata.putAll(transaction.getMetadata());
        }

        // Source tracing (used for deduplication and audit)
        metadata.put("deviceId", deviceId);
        metadata.put("transactionId", transaction.getTransactionId());
        metadata.put("ingestionSource", "MOBILE_APP");

        // Embed backend-verified hints as canonical carbon metadata
        if (finalHints != null) {
            Map<String, Object> hintsMap = new HashMap<>();
            if (finalHints.getActivityType()     != null) hintsMap.put("activityType",     finalHints.getActivityType().name());
            if (finalHints.getTransportMode()    != null) hintsMap.put("transportMode",    finalHints.getTransportMode().name());
            if (finalHints.getFuelType()         != null) hintsMap.put("fuelType",         finalHints.getFuelType().name());
            if (finalHints.getEnergySource()     != null) hintsMap.put("energySource",     finalHints.getEnergySource().name());
            if (finalHints.getCabinClass()       != null) hintsMap.put("cabinClass",       finalHints.getCabinClass().name());
            if (finalHints.getVehicleType()      != null) hintsMap.put("vehicleType",      finalHints.getVehicleType().name());
            if (finalHints.getMerchantIndustry() != null) hintsMap.put("merchantIndustry", finalHints.getMerchantIndustry().name());
            if (finalHints.getElectricityUnit()  != null) hintsMap.put("electricityUnit",  finalHints.getElectricityUnit());
            if (finalHints.getFuelUnit()         != null) hintsMap.put("fuelUnit",         finalHints.getFuelUnit());
            if (finalHints.getPassengerCount()   != null) hintsMap.put("passengerCount",   finalHints.getPassengerCount());
            if (finalHints.getEstimatedDistance()!= null) hintsMap.put("estimatedDistance",finalHints.getEstimatedDistance());
            if (finalHints.getConfidence()       != null) hintsMap.put("confidence",       finalHints.getConfidence());
            metadata.put(CARBON_HINTS_KEY, hintsMap);
        }

        return metadata;
    }
}
