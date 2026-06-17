package com.carbonfootprint.platform.ingestion.port.out;

import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.IngestionRequest;
import com.carbonfootprint.platform.platform.exception.IngestionException;

/**
 * Outbound port: abstraction for all data ingestion sources.
 *
 * <h3>Open/Closed Principle</h3>
 * Every new data source (receipt, Gmail, SMS, bank statement, IoT) implements
 * this interface. The ingestion pipeline ({@link com.carbonfootprint.platform.ingestion.service.IngestionPipelineService})
 * never changes when a new source is added.
 *
 * <h3>Source selection</h3>
 * The pipeline iterates over all registered {@code IngestionSource} beans and
 * calls {@link #supports(IngestionRequest)} to find the correct one. Sources
 * are selected by Spring DI — no custom factory is needed.
 *
 * <h3>Responsibility</h3>
 * An {@code IngestionSource} is responsible ONLY for converting external input
 * into a {@link RawDocument}. It must NOT parse, validate, or normalise.
 *
 * <p>Implementations:
 * <ul>
 *   <li>{@link com.carbonfootprint.platform.integration.receipt.ReceiptIngestionSource} (Phase 1)</li>
 *   <li>{@link com.carbonfootprint.platform.integration.gmail.GmailIngestionSource} (Phase 2)</li>
 *   <li>SmsIngestionSource (Future — Android Companion App)</li>
 * </ul>
 */
public interface IngestionSource {

    /**
     * Returns the {@link ActivitySource} this implementation handles.
     * Used for logging and traceability.
     */
    ActivitySource getSource();

    /**
     * Determines whether this source can handle the given request.
     *
     * <p>Implementations should inspect {@code request.getSource()} and/or
     * {@code request.getMimeType()} to make this determination.
     *
     * @param request the incoming ingestion request
     * @return {@code true} if this source can process the request
     */
    boolean supports(IngestionRequest request);

    /**
     * Converts the external input into a {@link RawDocument}.
     *
     * <p>For receipt sources: invoke the OCR provider and populate rawText.
     * For email sources: extract the email body and attachments.
     * For SMS sources: pass through the text directly.
     *
     * @param request the ingestion request
     * @return a populated {@link RawDocument} ready for parsing
     * @throws IngestionException if extraction fails (e.g., OCR error, API failure)
     */
    RawDocument ingest(IngestionRequest request) throws IngestionException;
}
