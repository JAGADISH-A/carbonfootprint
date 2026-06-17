package com.carbonfootprint.platform.ingestion.service;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.document.port.out.RawDocumentRepository;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.ingestion.model.IngestionLifecycleStage;
import com.carbonfootprint.platform.ingestion.model.IngestionRequest;
import com.carbonfootprint.platform.ingestion.model.IngestionResult;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;
import com.carbonfootprint.platform.ingestion.normalization.ActivityNormalizer;
import com.carbonfootprint.platform.ingestion.normalization.ActivityCarbonEnricher;
import com.carbonfootprint.platform.ingestion.normalization.ExtractionResultNormalizer;
import com.carbonfootprint.platform.ingestion.normalization.ExtractionResultToActivityConverter;
import com.carbonfootprint.platform.ingestion.port.in.IngestionUseCase;
import com.carbonfootprint.platform.ingestion.port.out.DocumentParser;
import com.carbonfootprint.platform.ingestion.port.out.IngestionSource;
import com.carbonfootprint.platform.ingestion.validation.ExtractionResultValidator;
import com.carbonfootprint.platform.ingestion.validation.RawDocumentValidator;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import com.carbonfootprint.platform.shared.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Core orchestrator of the Activity Ingestion Pipeline.
 *
 * <h3>Pipeline Steps</h3>
 * <ol>
 *   <li><strong>Source resolution</strong> — finds the {@link IngestionSource} that supports the request.</li>
 *   <li><strong>Document extraction</strong> — calls {@link IngestionSource#ingest} to produce a {@link RawDocument}.</li>
 *   <li><strong>RawDocument persistence</strong> — saves the raw document immediately for audit/reprocessing.</li>
 *   <li><strong>RawDocument validation</strong> — runs all {@link RawDocumentValidator}s in order; halts on failure.</li>
 *   <li><strong>Parsing</strong> — finds the {@link DocumentParser} that supports the document and extracts an {@link ExtractionResult}.</li>
 *   <li><strong>ExtractionResult normalization</strong> — normalizes currency, merchant, amount, category, dates, metadata, and recomputes confidence.</li>
 *   <li><strong>Carbon enrichment</strong> — infers fuel type, transport mode, cabin class, merchant industry, and other carbon-domain hints via {@link ActivityCarbonEnricher}.</li>
 *   <li><strong>ExtractionResult validation</strong> — runs all {@link ExtractionResultValidator}s; halts on failure.</li>
 *   <li><strong>Conversion</strong> — converts the validated {@link ExtractionResult} into an {@link Activity}.</li>
 *   <li><strong>Normalisation</strong> — runs all {@link ActivityNormalizer}s in order.</li>
 *   <li><strong>Activity persistence</strong> — saves the normalised activity.</li>
 * </ol>
 *
 * <h3>Extensibility</h3>
 * New sources, validators, parsers, and normalizers are registered as Spring
 * beans and injected automatically — this service does not change.
 *
 * <h3>Dependency Injection</h3>
 * All dependencies are constructor-injected ({@code @RequiredArgsConstructor}).
 * No field injection. All collaborators depend on interfaces, not implementations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionPipelineService implements IngestionUseCase {

    private final List<IngestionSource>      ingestionSources;
    private final List<RawDocumentValidator> rawDocumentValidators;
    private final List<DocumentParser>       documentParsers;
    private final ExtractionResultNormalizer extractionResultNormalizer;
    private final ActivityCarbonEnricher      activityCarbonEnricher;
    private final List<ExtractionResultValidator> extractionResultValidators;
    private final ExtractionResultToActivityConverter extractionResultToActivityConverter;
    private final List<ActivityNormalizer>   activityNormalizers;
    private final RawDocumentRepository      rawDocumentRepository;
    private final ActivityRepository         activityRepository;

    @Override
    public IngestionResult ingest(IngestionRequest request) {
        log.info("Starting ingestion pipeline for userId={} source={}",
                request.getUserId(), request.getSource());

        // ── Step 1: Resolve ingestion source ──────────────────────────────
        IngestionSource source = resolveSource(request);

        // ── Step 2: Extract RawDocument ───────────────────────────────────
        RawDocument rawDocument = extractDocument(source, request);

        // ── Step 3: Persist RawDocument immediately (audit trail) ─────────
        rawDocument = rawDocumentRepository.save(rawDocument);
        log.debug("Persisted RawDocument id={}", rawDocument.getId());

        // ── Step 4: Validate RawDocument ──────────────────────────────────
        ValidationResult validationResult = runDocumentValidators(rawDocument, IngestionLifecycleStage.RECEIVED);
        if (!validationResult.isValid()) {
            log.warn("RawDocument validation failed for id={}: {}",
                    rawDocument.getId(), validationResult.getViolations());
            return IngestionResult.failure(rawDocument,
                    "Validation failed: " + String.join("; ", validationResult.getViolations()));
        }

        // ── Step 5: Parse RawDocument into ExtractionResult ────────────────
        DocumentParser parser = resolveParser(rawDocument);
        ExtractionResult extractionResult = parseDocument(parser, rawDocument);

        log.debug("ExtractionResult created — parser={} confidence={}", 
                extractionResult.getParserName(), extractionResult.getConfidence());

        // ── Step 6: Normalize ExtractionResult ─────────────────────────────
        ExtractionResult normalizedExtractionResult = extractionResultNormalizer.normalize(extractionResult);
        log.debug("ExtractionResult normalized — confidence={}",
                normalizedExtractionResult.getConfidence());

        // ── Step 6b: Enrich ExtractionResult with carbon hints ─────────────
        ExtractionResult enrichedExtractionResult = activityCarbonEnricher.enrich(normalizedExtractionResult);
        log.debug("ExtractionResult enriched — carbonHints present={}",
                enrichedExtractionResult.getMetadata().containsKey("carbonHints"));

        // ── Step 7: Validate ExtractionResult ──────────────────────────────
        ValidationResult extractionValidationResult = runExtractionValidators(enrichedExtractionResult);
        if (!extractionValidationResult.isValid()) {
            log.warn("Extraction validation failed for documentId={}: {}",
                    rawDocument.getId(), extractionValidationResult.getViolations());
            return IngestionResult.failure(rawDocument,
                    "Extraction validation failed: " + String.join("; ", extractionValidationResult.getViolations()));
        }

        log.debug("Extraction validation completed");

        // ── Step 8: Convert to Activity ────────────────────────────────────
        Activity parsedActivity = extractionResultToActivityConverter.convert(
                enrichedExtractionResult,
                rawDocument,
                request.getUserId()
        );

        log.debug("Activity conversion completed");

        // ── Step 9: Normalise Activity ─────────────────────────────────────
        Activity normalisedActivity = normalise(parsedActivity);
        log.debug("Activity normalised — merchant={} category={} currency={}",
                normalisedActivity.getMerchant(),
                normalisedActivity.getCategory(),
                normalisedActivity.getCurrency());

        // ── Step 10: Persist Activity ──────────────────────────────────────
        Activity savedActivity = activityRepository.save(normalisedActivity);
        log.info("Ingestion pipeline completed. activityId={} userId={}",
                savedActivity.getId(), savedActivity.getUserId());

        return IngestionResult.success(rawDocument, savedActivity);
    }

    // ── Private pipeline steps ─────────────────────────────────────────────

    private IngestionSource resolveSource(IngestionRequest request) {
        return ingestionSources.stream()
                .filter(s -> s.supports(request))
                .findFirst()
                .orElseThrow(() -> new IngestionException(
                        "No IngestionSource found for source=" + request.getSource()
                                + " mimeType=" + request.getMimeType()
                ));
    }

    private RawDocument extractDocument(IngestionSource source, IngestionRequest request) {
        try {
            RawDocument doc = source.ingest(request);
            // Assign a new ID if the source did not set one
            if (doc.getId() == null) {
                doc = doc.withId(IdGenerator.generate());
            }
            return doc;
        } catch (IngestionException e) {
            throw e;
        } catch (Exception e) {
            throw new IngestionException("Unexpected error during document extraction: " + e.getMessage(), e);
        }
    }

    private ValidationResult runDocumentValidators(RawDocument document, IngestionLifecycleStage stage) {
        return rawDocumentValidators.stream()
                .map(v -> v.validate(document, stage))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private ValidationResult runExtractionValidators(ExtractionResult result) {
        return extractionResultValidators.stream()
                .map(v -> v.validate(result))
                .reduce(ValidationResult.ok(), ValidationResult::merge);
    }

    private DocumentParser resolveParser(RawDocument document) {
        return documentParsers.stream()
                .filter(p -> p.supports(document))
                .findFirst()
                .orElseThrow(() -> new IngestionException(
                        "No DocumentParser found for source=" + document.getSource()
                                + " mimeType=" + document.getMimeType()
                ));
    }

    private ExtractionResult parseDocument(DocumentParser parser, RawDocument document) {
        try {
            return parser.parse(document);
        } catch (IngestionException e) {
            throw e;
        } catch (Exception e) {
            throw new IngestionException("Unexpected error during ExtractionResult parsing: " + e.getMessage(), e);
        }
    }

    private Activity normalise(Activity activity) {
        Activity current = activity;
        for (ActivityNormalizer normalizer : activityNormalizers) {
            current = normalizer.normalize(current);
        }
        return current;
    }
}
