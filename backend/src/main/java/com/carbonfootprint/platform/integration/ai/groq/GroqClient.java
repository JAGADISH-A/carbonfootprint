package com.carbonfootprint.platform.integration.ai.groq;

import com.carbonfootprint.platform.platform.exception.IngestionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Reusable client for the Groq Chat Completions API (OpenAI-compatible).
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Accept a prompt string</li>
 *   <li>Send it to the Groq REST API with retry for transient failures</li>
 *   <li>Return the generated text</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * API key: {@code carbon.groq.api-key} (from environment variable GROQ_API_KEY).
 * Receipt model: {@code carbon.groq.receipt-model} (default: openai/gpt-oss-20b).
 * Coach model: {@code carbon.groq.coach-model} (default: llama-3.3-70b-versatile).
 * Base URL: {@code carbon.groq.base-url} (default: https://api.groq.com).
 * Timeout: {@code carbon.groq.timeout-seconds} (default: 60).
 */
@Slf4j
@Component
@Profile("!stub")
public class GroqClient {

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 500;

    private final String defaultModel;
    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GroqClient(
            @Value("${carbon.groq.receipt-model:openai/gpt-oss-20b}") String defaultModel,
            @Value("${carbon.groq.api-key:}") String apiKey,
            @Value("${carbon.groq.base-url:https://api.groq.com}") String baseUrl,
            @Value("${carbon.groq.timeout-seconds:60}") int timeoutSeconds,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.defaultModel = defaultModel;
        this.apiKey = apiKey;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .defaultHeaders(headers -> {
                    headers.setBearerAuth(apiKey != null ? apiKey : "");
                    headers.setContentType(MediaType.APPLICATION_JSON);
                })
                .build();

        if (this.apiKey == null || this.apiKey.isBlank()) {
            log.warn("GroqClient initialised with empty API key — chat completions will fail");
        } else {
            log.info("GroqClient initialised with defaultModel={} baseUrl={} timeout={}s", defaultModel, baseUrl, timeoutSeconds);
        }
    }

    /**
     * Default system message for extraction-oriented calls.
     */
    private static final String EXTRACTION_SYSTEM_MESSAGE =
            "Extract information from documents. Return ONLY valid JSON. No markdown. Use null for unknown fields. "
            + "Numeric fields must contain final computed values only — never arithmetic expressions (e.g. use 1440.48, not 1375.00 + 32.74 + 32.74). "
            + "Never output comments, explanatory text, or code fences. Every value must be a literal JSON primitive.";

    /**
     * Sends the given prompt to the Groq API with the default extraction system message
     * and the default model.
     * Retries on transient failures (408, 429, 5xx, connection/timeout errors)
     * with exponential backoff.
     *
     * @param prompt the complete prompt to send as the user message
     * @return the raw Groq API response body
     * @throws AiQuotaExceededException  if 429 after retries exhausted
     * @throws AiTimeoutException        if 408 / connection timeout after retries exhausted
     * @throws AiProviderException       if 5xx after retries, non-retryable 4xx, or config error
     */
    public String generateContent(String prompt) throws IngestionException {
        return generateContent(defaultModel, EXTRACTION_SYSTEM_MESSAGE, prompt);
    }

    /**
     * Sends the given prompt to the Groq API with the default extraction system message
     * and a specified model.
     * Retries on transient failures (408, 429, 5xx, connection/timeout errors)
     * with exponential backoff.
     *
     * @param model   the model identifier to use for this request
     * @param prompt  the complete prompt to send as the user message
     * @return the raw Groq API response body
     * @throws AiQuotaExceededException  if 429 after retries exhausted
     * @throws AiTimeoutException        if 408 / connection timeout after retries exhausted
     * @throws AiProviderException       if 5xx after retries, non-retryable 4xx, or config error
     */
    public String generateContent(String model, String prompt) throws IngestionException {
        return generateContent(model, EXTRACTION_SYSTEM_MESSAGE, prompt);
    }

    /**
     * Sends the given prompt to the Groq API with a custom system message
     * and the default model.
     * Retries on transient failures (408, 429, 5xx, connection/timeout errors)
     * with exponential backoff.
     *
     * @param systemMessage the system-level instruction for the model
     * @param userPrompt    the user message content
     * @return the raw Groq API response body
     * @throws AiQuotaExceededException  if 429 after retries exhausted
     * @throws AiTimeoutException        if 408 / connection timeout after retries exhausted
     * @throws AiProviderException       if 5xx after retries, non-retryable 4xx, or config error
     */
    public String generateContent(String systemMessage, String userPrompt) throws IngestionException {
        return generateContent(defaultModel, systemMessage, userPrompt);
    }

    /**
     * Sends the given prompt to the Groq API with a custom system message and model.
     * Retries on transient failures (408, 429, 5xx, connection/timeout errors)
     * with exponential backoff.
     *
     * @param model         the model identifier to use for this request
     * @param systemMessage the system-level instruction for the model
     * @param userPrompt    the user message content
     * @return the raw Groq API response body
     * @throws AiQuotaExceededException  if 429 after retries exhausted
     * @throws AiTimeoutException        if 408 / connection timeout after retries exhausted
     * @throws AiProviderException       if 5xx after retries, non-retryable 4xx, or config error
     */
    public String generateContent(String model, String systemMessage, String userPrompt) throws IngestionException {
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new AiProviderException("Prompt must not be null or blank", 0);
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiProviderException("Groq API key is not configured", 0);
        }

        log.debug("GroqClient.generateContent() — model={} promptLength={}", model, userPrompt.length());

        List<GroqMessage> messages = List.of(
                GroqMessage.system(systemMessage),
                GroqMessage.user(userPrompt)
        );

        GroqChatCompletionRequest requestBody = GroqChatCompletionRequest.of(
                model, messages, 0.1, 4096
        );

        Instant start = Instant.now();
        String rawResponseBody = executeWithRetry(requestBody);
        long latencyMs = Duration.between(start, Instant.now()).toMillis();

        log.info("Groq API call completed — model={} latencyMs={} responseSize={}",
                model, latencyMs, rawResponseBody != null ? rawResponseBody.length() : 0);

        return rawResponseBody;
    }

    private String executeWithRetry(GroqChatCompletionRequest requestBody) throws IngestionException {
        int lastRetryableStatus = 0;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                String responseBody = restClient.post()
                        .uri("/openai/v1/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                if (responseBody == null || responseBody.isBlank()) {
                    throw new AiProviderException("Groq API returned empty response", 0);
                }

                return responseBody;

            } catch (IngestionException e) {
                throw e;
            } catch (RestClientResponseException e) {
                lastRetryableStatus = e.getStatusCode().value();
                String errorBody = e.getResponseBodyAsString();

                if (lastRetryableStatus == 429) {
                    log.warn("Groq API attempt {}/{} failed with HTTP 429 (rate-limit) — retrying. Body: {}",
                            attempt, MAX_RETRIES, truncateBody(errorBody));
                    backoff(attempt);
                } else if (lastRetryableStatus == 408) {
                    log.warn("Groq API attempt {}/{} failed with HTTP 408 (timeout) — retrying. Body: {}",
                            attempt, MAX_RETRIES, truncateBody(errorBody));
                    backoff(attempt);
                } else if (lastRetryableStatus >= 500) {
                    log.warn("Groq API attempt {}/{} failed with HTTP {} (server error) — retrying. Body: {}",
                            attempt, MAX_RETRIES, lastRetryableStatus, truncateBody(errorBody));
                    backoff(attempt);
                } else {
                    // Non-retryable 4xx (400, 401, 403, etc.)
                    log.error("Groq API returned non-retryable HTTP {}: {}", lastRetryableStatus, truncateBody(errorBody));
                    throw new AiProviderException(
                            "Groq API returned HTTP " + lastRetryableStatus + ": " + truncateBody(errorBody),
                            lastRetryableStatus, e);
                }
            } catch (RestClientException e) {
                // Connection error, socket timeout, etc.
                log.warn("Groq API attempt {}/{} failed with connection error — retrying: {}",
                        attempt, MAX_RETRIES, e.getMessage());
                lastRetryableStatus = 0;
                backoff(attempt);
            } catch (Exception e) {
                throw new AiProviderException("Unexpected error calling Groq: " + e.getMessage(), 0, e);
            }
        }

        // All retries exhausted — throw the appropriate typed exception
        throwRetryExhausted(lastRetryableStatus);
        return unreachable();
    }

    /**
     * Throws the correct typed exception after retries are exhausted,
     * based on the last HTTP status code encountered.
     */
    private void throwRetryExhausted(int lastStatus) throws IngestionException {
        if (lastStatus == 429) {
            throw new AiQuotaExceededException(
                    "Groq API rate-limit (429) exceeded after " + MAX_RETRIES + " retries", lastStatus);
        } else if (lastStatus == 408) {
            throw new AiTimeoutException(
                    "Groq API request timed out (408) after " + MAX_RETRIES + " retries", lastStatus);
        } else if (lastStatus >= 500) {
            throw new AiProviderException(
                    "Groq API server error (HTTP " + lastStatus + ") after " + MAX_RETRIES + " retries", lastStatus);
        } else {
            // Connection error (status == 0)
            throw new AiProviderException(
                    "Groq API connection failed after " + MAX_RETRIES + " retries", 0);
        }
    }

    private static String truncateBody(String body) {
        if (body == null) return "null";
        return body.length() <= 300 ? body : body.substring(0, 300) + "...";
    }

    private static String unreachable() {
        throw new AssertionError("Should never reach here");
    }

    private void backoff(int attempt) {
        long delay = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Retry backoff interrupted");
        }
    }
}
