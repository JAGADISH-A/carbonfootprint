package com.carbonfootprint.platform.platform.exception;

/**
 * Thrown when any step of the ingestion pipeline fails.
 *
 * <p>Examples:
 * <ul>
 *   <li>No {@link com.carbonfootprint.platform.ingestion.port.out.IngestionSource} found for source type.</li>
 *   <li>OCR service returned an error or timed out.</li>
 *   <li>No {@link com.carbonfootprint.platform.ingestion.port.out.DocumentParser} found for document type.</li>
 *   <li>Gemini API call failed.</li>
 * </ul>
 */
public class IngestionException extends CarbonPlatformException {

    public IngestionException(String message) {
        super(message);
    }

    public IngestionException(String message, Throwable cause) {
        super(message, cause);
    }
}
