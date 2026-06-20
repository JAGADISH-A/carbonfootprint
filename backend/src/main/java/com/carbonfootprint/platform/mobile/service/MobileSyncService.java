package com.carbonfootprint.platform.mobile.service;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.carbonfootprint.platform.ingestion.enrichment.CarbonHintEngine;
import com.carbonfootprint.platform.ingestion.model.CarbonHints;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.ingestion.normalization.ActivityNormalizer;
import com.carbonfootprint.platform.mobile.dto.EnrichedTransaction;
import com.carbonfootprint.platform.mobile.dto.MobileSyncRequest;
import com.carbonfootprint.platform.mobile.dto.MobileSyncResponse;
import com.carbonfootprint.platform.mobile.mapper.MobileTransactionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the Mobile Synchronization pipeline.
 *
 * <h3>Pipeline per transaction:</h3>
 * <ol>
 *   <li>Deduplication — skip if already ingested (by deviceId + transactionId)</li>
 *   <li>Conversion — map {@link EnrichedTransaction} to {@link ExtractionResult}</li>
 *   <li>CarbonHintEngine — validate Android ML hints, fill any gaps</li>
 *   <li>Mapping — produce {@link Activity} with final backend-verified hints</li>
 *   <li>Normalization — reuse the existing {@link ActivityNormalizer} chain</li>
 *   <li>Persistence — save via the shared {@link ActivityRepository} (Firestore)</li>
 * </ol>
 *
 * <p>This service contains <em>no independent business logic</em>. All carbon hint
 * resolution, normalization, and persistence reuse the same Spring beans used by the
 * receipt ingestion pipeline, ensuring mobile activities appear in the unified dashboard.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileSyncService {

    private final MobileTransactionMapper mapper;
    private final CarbonHintEngine carbonHintEngine;
    private final List<ActivityNormalizer> activityNormalizers;
    private final ActivityRepository activityRepository;

    /**
     * Processes a batch of enriched transactions from the Android Companion App.
     *
     * @param request the validated sync request
     * @param userId  the authenticated user identifier
     * @return a {@link MobileSyncResponse} summarising counts, IDs, and any warnings
     */
    public MobileSyncResponse processSync(MobileSyncRequest request, String userId) {
        Instant startTime = Instant.now();
        String deviceId = request.getDeviceId();
        String syncId   = request.getSyncId();

        log.info("MobileSyncService — starting sync: syncId={} deviceId={} userId={} transactionCount={}",
                syncId, deviceId, userId, request.getTransactions().size());

        int received = request.getTransactions().size();
        int duplicateCount = 0;
        int failedCount = 0;
        List<String> activityIds  = new ArrayList<>();
        List<String> failedIds    = new ArrayList<>();
        List<String> warnings     = new ArrayList<>();

        for (EnrichedTransaction transaction : request.getTransactions()) {
            String txId = transaction.getTransactionId();
            try {
                // ── 1. Deduplication ───────────────────────────────────────
                if (activityRepository.existsByMobileTransaction(userId, deviceId, txId)) {
                    log.debug("MobileSyncService — duplicate skipped: transactionId={} deviceId={}", txId, deviceId);
                    duplicateCount++;
                    warnings.add("Skipped duplicate transaction: " + txId);
                    continue;
                }

                // ── 2. Convert to ExtractionResult for the hint engine ─────
                ExtractionResult extractionResult = mapper.toExtractionResult(transaction);
                log.debug("MobileSyncService — converted to ExtractionResult: txId={} merchant={} category={}",
                        txId, extractionResult.getMerchant(), extractionResult.getCategory());

                // ── 3. Backend CarbonHintEngine — validate + complete hints ─
                // The engine reads existing carbonHints from metadata as seed (Phase 4.1),
                // then runs all providers to fill any gaps the Android ML model missed.
                CarbonHints finalHints = carbonHintEngine.computeHints(extractionResult);
                log.debug("MobileSyncService — hints computed: txId={} activityType={} confidence={}",
                        txId, finalHints.getActivityType(), finalHints.getConfidence());

                // ── 4. Map to Activity with verified hints ─────────────────
                Activity activity = mapper.toActivity(transaction, finalHints, userId, deviceId);

                // ── 5. Normalize (currency, unit, merchant, date, location) ─
                activity = runNormalizers(activity);
                log.debug("MobileSyncService — normalized: activityId={} merchant={} currency={}",
                        activity.getId(), activity.getMerchant(), activity.getCurrency());

                // ── 6. Persist via shared Firestore repository ─────────────
                Activity saved = activityRepository.save(activity);
                activityIds.add(saved.getId());
                log.info("MobileSyncService — saved activity: activityId={} txId={} category={} source={}",
                        saved.getId(), txId, saved.getCategory(), saved.getSource());

            } catch (Exception e) {
                log.error("MobileSyncService — failed to process transaction: txId={} error={}", txId, e.getMessage(), e);
                failedCount++;
                failedIds.add(txId);
                warnings.add("Failed to process transaction " + txId + ": " + e.getMessage());
            }
        }

        long processingMs = Duration.between(startTime, Instant.now()).toMillis();
        int processedCount = activityIds.size();

        log.info("MobileSyncService — sync complete: syncId={} received={} processed={} duplicates={} failed={} durationMs={}",
                syncId, received, processedCount, duplicateCount, failedCount, processingMs);

        return MobileSyncResponse.builder()
                .syncId(syncId)
                .processedCount(processedCount)
                .successCount(processedCount)
                .failureCount(failedCount)
                .failedTransactionIds(failedIds)
                .build();
    }

    // ── Private helpers ─────────────────────────────────────────────────────

    /**
     * Runs the full {@link ActivityNormalizer} chain (same chain used by receipt ingestion).
     * Each normalizer returns a new immutable {@link Activity} instance via Lombok {@code @With}.
     */
    private Activity runNormalizers(Activity activity) {
        Activity current = activity;
        for (ActivityNormalizer normalizer : activityNormalizers) {
            try {
                current = normalizer.normalize(current);
            } catch (Exception e) {
                log.warn("MobileSyncService — normalizer {} threw exception, continuing: {}",
                        normalizer.getClass().getSimpleName(), e.getMessage());
            }
        }
        return current;
    }
}
