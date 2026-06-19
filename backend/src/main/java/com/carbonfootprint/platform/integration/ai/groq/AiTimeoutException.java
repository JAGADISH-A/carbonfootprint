package com.carbonfootprint.platform.integration.ai.groq;

import com.carbonfootprint.platform.platform.exception.IngestionException;

/**
 * Thrown when the AI provider returns HTTP 408 (Request Timeout)
 * or a connection/socket timeout occurs, and all retries have been exhausted.
 *
 * <h3>Retry behaviour</h3> The {@link GroqClient} retries up to 3 times with
 * exponential backoff before throwing this exception.
 */
public class AiTimeoutException extends IngestionException {

    private final int statusCode;

    public AiTimeoutException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AiTimeoutException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
