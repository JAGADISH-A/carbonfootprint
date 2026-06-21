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
import com.carbonfootprint.platform.mobile.dto.BatchSyncRequest;
import com.carbonfootprint.platform.mobile.dto.BatchSyncResponse;
import com.carbonfootprint.platform.mobile.dto.BatchSyncItemResponse;
import com.carbonfootprint.platform.mobile.model.PendingActivity;
import com.carbonfootprint.platform.mobile.model.PendingActivityStatus;
import com.carbonfootprint.platform.mobile.port.out.PendingActivityRepository;
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
    private final PendingActivityRepository pendingActivityRepository;
    private final BatchEnrichmentService batchEnrichmentService;

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

    public BatchSyncResponse processBatch(BatchSyncRequest request, String userId) {
        log.info("[MOBILE_SYNC_PIPELINE] MobileSyncService - validating and deduplicating batch sync: deviceId={} syncSessionId={} items={} userId={}",
                request.getDeviceId(), request.getSyncSessionId(), request.getItems().size(), userId);

        List<BatchSyncItemResponse> responses = new ArrayList<>();
        List<String> savedPendingActivityIds = new ArrayList<>();

        for (var item : request.getItems()) {
            try {
                PendingActivity pendingActivity = PendingActivity.builder()
                        .id(item.getId())
                        .userId(userId)
                        .deviceId(request.getDeviceId())
                        .syncSessionId(request.getSyncSessionId())
                        .source(item.getSource() != null ? item.getSource() : "MOBILE_SMS")
                        .rawPayload(item.getMessageBody())
                        .merchant(item.getNormalizedMerchant())
                        .amount(null) // Amount typically needs parsing, leaving null if not provided
                        .timestamp(Instant.ofEpochMilli(item.getReceivedTimestamp()))
                        .retryCount(0)
                        .status(PendingActivityStatus.NEW)
                        .createdAt(Instant.now())
                        .build();

                log.debug("[MOBILE_SYNC_PIPELINE] MobileSyncService - saving pending activity: id={}", item.getId());
                pendingActivityRepository.upsert(pendingActivity);
                savedPendingActivityIds.add(pendingActivity.getId());

                responses.add(BatchSyncItemResponse.builder()
                        .id(item.getId())
                        .status("SUCCESS")
                        .build());
            } catch (Exception e) {
                log.error("[MOBILE_SYNC_PIPELINE] MobileSyncService - failed to save pending activity: id={} error={}", item.getId(), e.getMessage(), e);
                responses.add(BatchSyncItemResponse.builder()
                        .id(item.getId())
                        .status("FAILED")
                        .reason(e.getMessage())
                        .build());
            }
        }

        BatchSyncResponse response = BatchSyncResponse.builder()
                .syncSessionId(request.getSyncSessionId())
                .results(responses)
                .build();

        log.info("[MOBILE_SYNC_PIPELINE] MobileSyncService - triggering async enrichment for {} saved items", savedPendingActivityIds.size());
        for (String id : savedPendingActivityIds) {
            batchEnrichmentService.enrichPendingActivity(id);
        }

        return response;
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
