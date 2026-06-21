package com.carbonfootprint.platform.mobile.service;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.carbonfootprint.platform.carbon.calculation.CarbonCalculationEngine;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.ingestion.normalization.ActivityNormalizer;
import com.carbonfootprint.platform.ingestion.normalization.ExtractionResultToActivityConverter;
import com.carbonfootprint.platform.integration.ai.groq.GroqDocumentParser;
import com.carbonfootprint.platform.mobile.model.PendingActivity;
import com.carbonfootprint.platform.mobile.model.PendingActivityStatus;
import com.carbonfootprint.platform.mobile.port.out.PendingActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchEnrichmentService {

    private final PendingActivityRepository pendingActivityRepository;
    private final GroqDocumentParser groqDocumentParser;
    private final ExtractionResultToActivityConverter extractionResultToActivityConverter;
    private final List<ActivityNormalizer> activityNormalizers;
    private final CarbonCalculationEngine carbonCalculationEngine;
    private final ActivityRepository activityRepository;

    @Async
    public void enrichPendingActivity(String pendingActivityId) {
        log.info("[MOBILE_SYNC_PIPELINE] BatchEnrichmentService processing started for pendingActivityId={}", pendingActivityId);

        Optional<PendingActivity> optionalPendingActivity = pendingActivityRepository.findById(pendingActivityId);
        if (optionalPendingActivity.isEmpty()) {
            log.error("[MOBILE_SYNC_PIPELINE] Failed to find pendingActivityId={}", pendingActivityId);
            return;
        }

        PendingActivity pendingActivity = optionalPendingActivity.get();

        try {
            // 1. Mark as PROCESSING
            pendingActivityRepository.updateStatus(pendingActivityId, PendingActivityStatus.PROCESSING);

            // 2. Map to RawDocument for GroqDocumentParser
            RawDocument rawDocument = RawDocument.builder()
                    .id(pendingActivity.getId())
                    .userId(pendingActivity.getUserId())
                    .source(ActivitySource.valueOf(pendingActivity.getSource()))
                    .rawText(pendingActivity.getRawPayload())
                    .mimeType("text/plain")
                    .createdAt(Instant.now())
                    .build();

            // 3. Groq Parsing
            ExtractionResult extractionResult = groqDocumentParser.parse(rawDocument);
            log.info("[MOBILE_SYNC_PIPELINE] BatchEnrichmentService parser complete for pendingActivityId={}", pendingActivityId);

            // 4. Convert to Activity
            Activity parsedActivity = extractionResultToActivityConverter.convert(extractionResult, rawDocument, pendingActivity.getUserId());

            // 5. Normalization
            Activity normalizedActivity = parsedActivity;
            for (ActivityNormalizer normalizer : activityNormalizers) {
                normalizedActivity = normalizer.normalize(normalizedActivity);
            }
            log.info("[MOBILE_SYNC_PIPELINE] BatchEnrichmentService normalization complete for pendingActivityId={}", pendingActivityId);

            // 6. Carbon Calculation
            Activity activityWithAssessment = calculateCarbon(normalizedActivity);
            log.info("[MOBILE_SYNC_PIPELINE] BatchEnrichmentService carbon calculation complete for pendingActivityId={}", pendingActivityId);

            // 7. Persist to activities collection
            // Set deviceId in metadata to preserve origin
            Map<String, Object> updatedMetadata = new HashMap<>(activityWithAssessment.getMetadata());
            updatedMetadata.put("deviceId", pendingActivity.getDeviceId());
            updatedMetadata.put("syncSessionId", pendingActivity.getSyncSessionId());
            activityWithAssessment = activityWithAssessment.withMetadata(updatedMetadata);
            
            activityRepository.save(activityWithAssessment);
            log.info("[MOBILE_SYNC_PIPELINE] BatchEnrichmentService activity persisted for pendingActivityId={}", pendingActivityId);

            // 8. Mark as PROCESSED
            pendingActivityRepository.updateStatus(pendingActivityId, PendingActivityStatus.PROCESSED);
            log.info("[MOBILE_SYNC_PIPELINE] BatchEnrichmentService pending status updated to PROCESSED for pendingActivityId={}", pendingActivityId);

        } catch (Exception e) {
            log.error("[MOBILE_SYNC_PIPELINE] BatchEnrichmentService exception for pendingActivityId={}: {}", pendingActivityId, e.getMessage(), e);
            pendingActivityRepository.updateFailure(pendingActivityId, e.getMessage());
            log.warn("[MOBILE_SYNC_PIPELINE] BatchEnrichmentService retry count incremented, last error updated for pendingActivityId={}", pendingActivityId);
        }
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
            log.warn("Carbon calculation failed for activity {}: {}", activity.getId(), e.getMessage());
        }
        return activity;
    }
}
