package com.carbonfootprint.platform.ingestion.port.out;

import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.platform.exception.IngestionException;

/**
 * Outbound port: abstraction for parsing a {@link RawDocument} into an
 * {@link ExtractionResult}.
 *
 * <h3>Architectural boundary</h3>
 * This port deliberately returns {@link ExtractionResult} — NOT
 * {@link com.carbonfootprint.platform.activity.model.Activity}. This keeps the
 * {@code Activity} domain model completely free of any dependency on AI providers,
 * OCR engines, or parsing strategies.
 *
 * <p>Pipeline position:
 * {@code RawDocument → DocumentParser → ExtractionResult → Validation → Normalization → Activity}
 *
 * <h3>Multiple implementations</h3>
 * <ul>
 *   <li>{@link com.carbonfootprint.platform.integration.ai.gemini.GeminiDocumentParser}
 *       — uses Gemini 2.0 Flash for semantic extraction (default).</li>
 *   <li>Future: {@code RegexDocumentParser} — fast, offline, pattern-based.</li>
 *   <li>Future: {@code RuleBasedDocumentParser} — deterministic, fully auditable.</li>
 *   <li>Future: {@code OfflineDocumentParser} — ML model bundled with the service.</li>
 * </ul>
 *
 * <h3>Selection via Spring DI</h3>
 * Use {@code @Primary} or {@code @Qualifier} on implementations.
 * No custom factory class required — Spring resolves the list automatically.
 */
public interface DocumentParser {

    /**
     * Returns {@code true} if this parser can handle the given document.
     *
     * <p>Implementations may inspect {@code document.getSource()},
     * {@code document.getMimeType()}, or {@code document.getLanguage()}.
     *
     * @param document the raw document to evaluate
     * @return {@code true} if this parser supports the document
     */
    boolean supports(RawDocument document);

    /**
     * Parses a {@link RawDocument} and extracts field values into an
     * {@link ExtractionResult}.
     *
     * <p>Contract:
     * <ul>
     *   <li>Fields that cannot be determined MUST be null — do not guess.</li>
     *   <li>The parser MUST NOT apply normalisation, validation, or defaults.</li>
     *   <li>The parser MUST NOT reference or create {@code Activity} objects.</li>
     *   <li>Set {@code ExtractionResult.confidence} to reflect extraction quality.</li>
     * </ul>
     *
     * @param document the raw document to parse
     * @return an {@link ExtractionResult} with extracted field values (may be partial)
     * @throws IngestionException if parsing fails unrecoverably
     */
    ExtractionResult parse(RawDocument document) throws IngestionException;
}
