package com.carbonfootprint.platform.integration.ai.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Reusable client for the Google Gemini text generation API.
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Accept a prompt string</li>
 *   <li>Send it to the Gemini REST API</li>
 *   <li>Return the generated text</li>
 * </ul>
 *
 * <h3>Design</h3>
 * <ul>
 *   <li>Single-responsibility — only handles HTTP transport and response extraction</li>
 *   <li>No prompt construction — callers provide the complete prompt</li>
 *   <li>No response parsing — callers receive raw text and handle parsing</li>
 *   <li>Reusable across document extraction, coaching, and any future Gemini use case</li>
 * </ul>
 *
 * <h3>Testability</h3>
 * Response parsing is extracted into {@link #parseResponseText(String)} so tests
 * can stub it via Mockito spy without needing to mock RestClient's generic types.
 *
 * <h3>Configuration</h3>
 * API key: {@code carbon.gemini.api-key}
 * Model: {@code carbon.gemini.model} (default: gemini-2.0-flash)
 *
 * @see GeminiDocumentParser
 */
@Slf4j
@Component
@Profile("!stub")
public class GeminiClient {

    private static final String BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String GENERATE_CONTENT_PATH = "/v1beta/models/%s:generateContent";

    private final String geminiModel;
    private final String geminiApiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiClient(
            @Value("${carbon.gemini.model:gemini-2.0-flash}") String geminiModel,
            @Value("${carbon.gemini.api-key:}") String geminiApiKey,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.geminiModel = geminiModel;
        this.geminiApiKey = geminiApiKey;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder
                .baseUrl(BASE_URL)
                .build();

        if (this.geminiApiKey == null || this.geminiApiKey.isBlank()) {
            log.warn("GeminiClient initialised with empty API key — generateContent will fail");
        } else {
            log.info("GeminiClient initialised with model={}", geminiModel);
        }
    }

    /**
     * Sends the given prompt to the Gemini API and returns the generated text.
     *
     * @param prompt the complete prompt to send to Gemini
     * @return the generated text response
     * @throws GeminiClientException if the API call fails or returns an error
     */
    public String generateContent(String prompt) throws GeminiClientException {
        if (prompt == null || prompt.isBlank()) {
            throw new GeminiClientException("Prompt must not be null or blank");
        }

        if (geminiApiKey == null || geminiApiKey.isBlank()) {
            throw new GeminiClientException("Gemini API key is not configured");
        }

        log.debug("GeminiClient.generateContent() — model={} promptLength={}", geminiModel, prompt.length());

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        String rawResponseBody;
        try {
            rawResponseBody = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path(String.format(GENERATE_CONTENT_PATH, geminiModel))
                            .queryParam("key", geminiApiKey)
                            .build())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException e) {
            throw new GeminiClientException(
                    "Gemini API returned HTTP error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(), e);
        } catch (RestClientException e) {
            throw new GeminiClientException(
                    "Connection failure or timeout when calling Gemini: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new GeminiClientException(
                    "Unexpected error calling Gemini: " + e.getMessage(), e);
        }

        if (rawResponseBody == null || rawResponseBody.isBlank()) {
            throw new GeminiClientException("Gemini API returned empty response");
        }

        return parseResponseText(rawResponseBody);
    }

    /**
     * Parses the raw Gemini JSON response and extracts the generated text.
     *
     * <p>Package-private to allow Mockito spy-based testing — tests can stub
     * this method to return canned responses without needing RestClient mocking.</p>
     *
     * @param responseJson the raw JSON response from Gemini
     * @return the generated text
     * @throws GeminiClientException if the response cannot be parsed or contains an error
     */
    String parseResponseText(String responseJson) throws GeminiClientException {
        try {
            JsonNode rootNode = objectMapper.readTree(responseJson);

            if (rootNode.has("error")) {
                String errMsg = rootNode.path("error").path("message").asText();
                throw new GeminiClientException("Gemini API returned error: " + errMsg);
            }

            String text = rootNode.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText(null);

            if (text == null || text.isBlank()) {
                throw new GeminiClientException("Gemini response contained no text content");
            }

            return text.trim();
        } catch (GeminiClientException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiClientException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }
}
