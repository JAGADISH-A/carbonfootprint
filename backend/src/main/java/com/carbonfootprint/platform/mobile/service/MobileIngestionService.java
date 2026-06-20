package com.carbonfootprint.platform.mobile.service;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.carbonfootprint.platform.carbon.calculation.CarbonCalculationEngine;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import com.carbonfootprint.platform.ingestion.normalization.ActivityNormalizer;
import com.carbonfootprint.platform.mobile.dto.EnrichedTransaction;
import com.carbonfootprint.platform.mobile.mapper.MobileTransactionMapper;
import com.carbonfootprint.platform.mobile.validation.MobileTransactionValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Orchestrator for the Mobile Ingestion pipeline.
 *
 * <p>This service receives pre-extracted {@link EnrichedTransaction} objects from the Android app,
 * bypassing the raw document and OCR parsing stages. It integrates cleanly into the existing
 * downstream processing pipeline by reusing normalizers, carbon calculation, and persistence.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileIngestionService {

    private final MobileTransactionValidator validator;
    private final MobileTransactionMapper mapper;
    private final List<ActivityNormalizer> activityNormalizers;
    private final CarbonCalculationEngine carbonCalculationEngine;
    private final ActivityRepository activityRepository;

    /**
     * Processes an enriched transaction from the mobile client.
     *
     * @param transaction the incoming transaction DTO
     * @param userId      the authenticated user ID
     * @return the saved, normalized, and carbon-calculated Activity
     */
    public Activity processTransaction(EnrichedTransaction transaction, String userId) {
        log.info("Starting Mobile Ingestion pipeline for userId={}", userId);

        // 1. Validation
        validator.validateOrThrow(transaction);
        log.debug("Mobile transaction validated successfully.");

        // 2. Conversion (no deviceId in this legacy path — use sentinel; hints computed below via CarbonCalculationEngine)
        Activity parsedActivity = mapper.toActivity(transaction, null, userId, "unknown");
        log.debug("Converted EnrichedTransaction to Activity id={}", parsedActivity.getId());

        // 3. Normalization (reusing the existing pipeline chain)
        Activity normalizedActivity = normalize(parsedActivity);
        log.debug("Activity normalized — merchant={} category={} currency={}",
                normalizedActivity.getMerchant(),
                normalizedActivity.getCategory(),
                normalizedActivity.getCurrency());

        // 4. Carbon Calculation
        Activity activityWithAssessment = calculateCarbon(normalizedActivity);
        log.debug("Carbon calculation completed.");

        // 5. Persistence
        Activity savedActivity = activityRepository.save(activityWithAssessment);
        log.info("Mobile Ingestion pipeline completed. activityId={} userId={}",
                savedActivity.getId(), savedActivity.getUserId());

        return savedActivity;
    }

    private Activity normalize(Activity activity) {
        Activity current = activity;
        for (ActivityNormalizer normalizer : activityNormalizers) {
            current = normalizer.normalize(current);
        }
        return current;
    }

    private Activity calculateCarbon(Activity activity) {
        try {
            Optional<EmissionResult> emissionResult = carbonCalculationEngine.calculate(activity);
            if (emissionResult.isPresent()) {
                EmissionResult result = emissionResult.get();
                Map<String, Object> assessment = new HashMap<>();
                assessment.put("carbonKg", result.getCarbonKg());
                assessment.put("methodology", result.getMethodology());
                assessment.put("emissionFactorValue", result.getEmissionFactorValue());
                assessment.put("emissionFactorSource", result.getEmissionFactorSource());
                assessment.put("emissionFactorUnit", result.getActivityUnit());
                assessment.put("activityQuantity", result.getActivityQuantity());
                assessment.put("calculatedAt", result.getCalculatedAt());

                Map<String, Object> updatedMetadata = new HashMap<>(activity.getMetadata());
                updatedMetadata.put("carbonAssessment", assessment);
                return activity.withMetadata(updatedMetadata);
            }
        } catch (Exception e) {
            log.warn("Carbon calculation failed for activity {}: {}",
                    activity.getId(), e.getMessage());
        }
        return activity;
    }
}
