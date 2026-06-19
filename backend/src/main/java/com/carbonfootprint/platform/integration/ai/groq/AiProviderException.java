package com.carbonfootprint.platform.integration.ai.groq;

import com.carbonfootprint.platform.platform.exception.IngestionException;

/**
 * Thrown when the AI provider returns a non-retryable HTTP error
 * (e.g., 400 Bad Request, 401 Unauthorized, 403 Forbidden) or when
 * all retries for a 5xx error have been exhausted.
 *
 * <h3>Typical causes</h3>
 * <ul>
 *   <li>Invalid API key (401)</li>
 *   <li>Malformed request body (400)</li>
 *   <li>Server-side failure after retries (500, 502, 503, 504)</li>
 * </ul>
 *
 * <p>For HTTP 429, see {@link AiQuotaExceededException}.
 * For HTTP 408 / connection timeouts, see {@link AiTimeoutException}.
 */
public class AiProviderException extends IngestionException {

    private final int statusCode;

    public AiProviderException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AiProviderException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
