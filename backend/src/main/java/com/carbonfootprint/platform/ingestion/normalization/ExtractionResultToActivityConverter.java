package com.carbonfootprint.platform.ingestion.normalization;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.shared.util.IdGenerator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Converts an {@link ExtractionResult} into a partial {@link Activity}.
 *
 * <h3>Role in the pipeline</h3>
 * This converter is the single, explicit bridge between the extraction DTO
 * and the domain model. It runs after ExtractionResult validation and before
 * the {@link ActivityNormalizer} chain.
 *
 * <p>Pipeline position:
 * <pre>
 * ExtractionResult → [ExtractionResultValidator chain] → ExtractionResultToActivityConverter
 *   → [ActivityNormalizer chain] → Activity
 * </pre>
 *
 * <h3>Defaults applied here</h3>
 * <ul>
 *   <li>{@link ActivityCategory#OTHER} when category is null.</li>
 *   <li>{@link ActivitySource#UNKNOWN} when source cannot be determined.</li>
 *   <li>New UUID generated for the activity id.</li>
 *   <li>{@code createdAt} set to current UTC time.</li>
 * </ul>
 *
 * <h3>What is NOT applied here</h3>
 * Normalization (merchant casing, ISO currency codes, UTC dates, canonical units)
 * is applied by the {@link ActivityNormalizer} chain AFTER this conversion.
 */
@Slf4j
@Component
public class ExtractionResultToActivityConverter {

    /**
     * Converts an {@link ExtractionResult} into a partial {@link Activity}.
     *
     * <p>The returned Activity will still be processed by the normalizer chain.
     * It may contain raw/un-normalised field values.
     *
     * @param result     the extraction result produced by a {@link com.carbonfootprint.platform.ingestion.port.out.DocumentParser}
     * @param rawDocument the source document (provides source, userId, rawDocumentId)
     * @param userId     the authenticated user id from the ingestion request
     * @return a partial Activity ready for the normalizer chain
     */
    public Activity convert(ExtractionResult result, RawDocument rawDocument, String userId) {
        log.debug("Converting ExtractionResult to Activity — parser={} category={} confidence={}",
                result.getParserName(), result.getCategory(), result.getConfidence());

        ActivityCategory resolvedCategory = result.getCategory() != null
                ? result.getCategory()
                : ActivityCategory.OTHER;

        ActivitySource resolvedSource = rawDocument.getSource() != null
                ? rawDocument.getSource()
                : ActivitySource.UNKNOWN;

        Activity activity = Activity.builder()
                .id(IdGenerator.generate())
                .userId(userId)
                .source(resolvedSource)
                .category(resolvedCategory)
                .merchant(result.getMerchant())
                .amount(result.getAmount())
                .currency(result.getCurrency())
                .unit(result.getUnit())
                .location(result.getLocation())
                .occurredAt(result.getOccurredAt())
                .description(result.getDescription())
                .rawDocumentId(rawDocument.getId())
                .metadata(result.getMetadata() != null ? result.getMetadata() : java.util.Map.of())
                .createdAt(Instant.now())
                .build();

        log.debug("ExtractionResult converted to Activity id={}", activity.getId());
        return activity;
    }
}
