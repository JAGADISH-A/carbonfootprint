package com.carbonfootprint.platform.integration.ai.groq;

import com.carbonfootprint.platform.platform.exception.IngestionException;

/**
 * Thrown when the AI provider returns JSON that cannot be deserialised
 * into an {@link com.carbonfootprint.platform.ingestion.model.ExtractionResult},
 * even after a single retry.
 *
 * <h3>Contract</h3>
 * This exception is ONLY thrown after:
 * <ol>
 *   <li>The raw response failed outer JSON validation, OR</li>
 *   <li>The extracted content string failed inner JSON validation.</li>
 *   <li>A single retry was attempted and also failed.</li>
 * </ol>
 *
 * <p>Malformed JSON never reaches the normalisation or activity-conversion layers.
 */
public class AiParsingException extends IngestionException {

    public AiParsingException(String message) {
        super(message);
    }

    public AiParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
