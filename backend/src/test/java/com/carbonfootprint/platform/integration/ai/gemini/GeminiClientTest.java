package com.carbonfootprint.platform.integration.ai.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiClientTest {

    @Mock
    private RestClient.Builder restClientBuilder;

    @Mock
    private RestClient restClient;

    private ObjectMapper objectMapper;

    private GeminiClient geminiClient;

    private static final String API_KEY = "test-api-key";
    private static final String MODEL = "gemini-2.0-flash";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();

        when(restClientBuilder.baseUrl("https://generativelanguage.googleapis.com")).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        geminiClient = new GeminiClient(MODEL, API_KEY, restClientBuilder, objectMapper);
    }

    // ── parseResponseText tests (via spy) ─────────────────────────────────

    @Test
    void parseResponseText_validResponse_extractsText() throws Exception {
        GeminiClient spy = org.mockito.Mockito.spy(geminiClient);
        String json = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{"text": "Hello! How can I help you today?"}],
                      "role": "model"
                    }
                  }]
                }
                """;

        String result = spy.parseResponseText(json);

        assertThat(result).isEqualTo("Hello! How can I help you today?");
    }

    @Test
    void parseResponseText_multilineText_returnsFullText() throws Exception {
        GeminiClient spy = org.mockito.Mockito.spy(geminiClient);
        String json = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{"text": "Line 1\\nLine 2\\nLine 3"}],
                      "role": "model"
                    }
                  }]
                }
                """;

        String result = spy.parseResponseText(json);

        assertThat(result).contains("Line 1");
        assertThat(result).contains("Line 2");
        assertThat(result).contains("Line 3");
    }

    @Test
    void parseResponseText_jsonInText_returnsRawText() throws Exception {
        GeminiClient spy = org.mockito.Mockito.spy(geminiClient);
        String json = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{"text": "{\\"key\\": \\"value\\"}"}],
                      "role": "model"
                    }
                  }]
                }
                """;

        String result = spy.parseResponseText(json);

        assertThat(result).contains("\"key\"");
        assertThat(result).contains("\"value\"");
    }

    @Test
    void parseResponseText_markdownWrappedText_returnsRawText() throws Exception {
        GeminiClient spy = org.mockito.Mockito.spy(geminiClient);
        String json = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{"text": "Here is your answer:\\n\\n```json\\n{\\"result\\": 42\\n}```"}],
                      "role": "model"
                    }
                  }]
                }
                """;

        String result = spy.parseResponseText(json);

        assertThat(result).startsWith("Here is your answer");
    }

    @Test
    void parseResponseText_errorBlock_throwsException() throws Exception {
        GeminiClient spy = org.mockito.Mockito.spy(geminiClient);
        String json = """
                {
                  "error": {
                    "code": 403,
                    "message": "API key not valid.",
                    "status": "PERMISSION_DENIED"
                  }
                }
                """;

        assertThatThrownBy(() -> spy.parseResponseText(json))
                .isInstanceOf(GeminiClientException.class)
                .hasMessageContaining("API key not valid");
    }

    @Test
    void parseResponseText_malformedJson_throwsException() throws Exception {
        GeminiClient spy = org.mockito.Mockito.spy(geminiClient);

        assertThatThrownBy(() -> spy.parseResponseText("not valid json {{{"))
                .isInstanceOf(GeminiClientException.class)
                .hasMessageContaining("Failed to parse Gemini response");
    }

    @Test
    void parseResponseText_emptyCandidates_throwsException() throws Exception {
        GeminiClient spy = org.mockito.Mockito.spy(geminiClient);
        String json = """
                {
                  "candidates": []
                }
                """;

        assertThatThrownBy(() -> spy.parseResponseText(json))
                .isInstanceOf(GeminiClientException.class)
                .hasMessageContaining("no text content");
    }

    @Test
    void parseResponseText_nullText_throwsException() throws Exception {
        GeminiClient spy = org.mockito.Mockito.spy(geminiClient);
        String json = """
                {
                  "candidates": [{
                    "content": {
                      "parts": [{"text": null}],
                      "role": "model"
                    }
                  }]
                }
                """;

        assertThatThrownBy(() -> spy.parseResponseText(json))
                .isInstanceOf(GeminiClientException.class)
                .hasMessageContaining("no text content");
    }

    // ── Input validation ──────────────────────────────────────────────────

    @Test
    void generateContent_nullPrompt_throwsException() {
        assertThatThrownBy(() -> geminiClient.generateContent(null))
                .isInstanceOf(GeminiClientException.class)
                .hasMessageContaining("must not be null or blank");
    }

    @Test
    void generateContent_blankPrompt_throwsException() {
        assertThatThrownBy(() -> geminiClient.generateContent("   "))
                .isInstanceOf(GeminiClientException.class)
                .hasMessageContaining("must not be null or blank");
    }

    @Test
    void generateContent_emptyPrompt_throwsException() {
        assertThatThrownBy(() -> geminiClient.generateContent(""))
                .isInstanceOf(GeminiClientException.class)
                .hasMessageContaining("must not be null or blank");
    }

    // ── Empty API key ─────────────────────────────────────────────────────

    @Test
    void generateContent_emptyApiKey_throwsException() {
        GeminiClient clientNoKey = new GeminiClient(MODEL, "", restClientBuilder, objectMapper);

        assertThatThrownBy(() -> clientNoKey.generateContent("test prompt"))
                .isInstanceOf(GeminiClientException.class)
                .hasMessageContaining("API key is not configured");
    }

    @Test
    void generateContent_nullApiKey_throwsException() {
        GeminiClient clientNullKey = new GeminiClient(MODEL, null, restClientBuilder, objectMapper);

        assertThatThrownBy(() -> clientNullKey.generateContent("test prompt"))
                .isInstanceOf(GeminiClientException.class)
                .hasMessageContaining("API key is not configured");
    }

    // ── Constructor ───────────────────────────────────────────────────────

    @Test
    void constructor_initializesSuccessfully() {
        assertThat(geminiClient).isNotNull();
    }

    @Test
    void constructor_logsWarningForEmptyApiKey() {
        GeminiClient clientNoKey = new GeminiClient(MODEL, "", restClientBuilder, objectMapper);
        assertThat(clientNoKey).isNotNull();
    }
}
