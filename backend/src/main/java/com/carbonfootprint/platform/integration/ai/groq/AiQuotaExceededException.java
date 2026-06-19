package com.carbonfootprint.platform.integration.ai.groq;

import com.carbonfootprint.platform.platform.exception.IngestionException;

/**
 * Thrown when the AI provider returns HTTP 429 (Too Many Requests)
 * and all retries have been exhausted.
 *
 * <h3>Typical cause</h3> Rate-limit exceeded — too many requests per second/minute.
 *
 * <h3>Retry behaviour</h3> The {@link GroqClient} retries up to 3 times with
 * exponential backoff before throwing this exception.
 */
public class AiQuotaExceededException extends IngestionException {

    private final int statusCode;

    public AiQuotaExceededException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AiQuotaExceededException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
