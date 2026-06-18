package com.carbonfootprint.platform.integration.ai.gemini;

/**
 * Exception thrown when the Gemini API call fails or returns an error.
 *
 * <h3>Design</h3>
 * Checked exception — callers must handle Gemini failures explicitly
 * (e.g., fall back to deterministic output).
 */
public class GeminiClientException extends Exception {

    public GeminiClientException(String message) {
        super(message);
    }

    public GeminiClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
