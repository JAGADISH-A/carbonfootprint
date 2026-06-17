package com.carbonfootprint.platform.integration.ocr.paddle;

import com.carbonfootprint.platform.integration.ocr.model.OcrResult;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaddleOcrProviderTest {

    private MockWebServer mockWebServer;
    private PaddleOcrProvider provider;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Speed up retries for testing
        PaddleOcrProvider.initialBackoffMs = 1;

        RestClient restClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();

        provider = new PaddleOcrProvider(restClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void testExtractText_Success() throws Exception {
        String jsonResponse = """
                {
                  "text": "Hello World Receipt",
                  "confidence": 0.95,
                  "language": "en",
                  "pageCount": 1
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        byte[] bytes = "receipt-content".getBytes();
        OcrResult result = provider.extractText(bytes, "application/pdf");

        assertThat(result).isNotNull();
        assertThat(result.getText()).isEqualTo("Hello World Receipt");
        assertThat(result.getConfidence()).isEqualTo(0.95);
        assertThat(result.getLanguage()).isEqualTo("en");
        assertThat(result.getMetadata()).containsEntry("provider", "PaddleOCR");
        assertThat(result.getMetadata()).containsEntry("language", "en");
        assertThat(result.getMetadata()).containsEntry("confidence", 0.95);
        assertThat(result.getMetadata()).containsEntry("pageCount", 1);
        assertThat(result.getMetadata()).containsKey("processingTimeMs");
        assertThat(result.getMetadata()).containsKey("requestId");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).isEqualTo("/ocr");
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getHeader("Content-Type")).contains("multipart/form-data");
        
        String body = request.getBody().readUtf8();
        assertThat(body).contains("filename=\"document.pdf\"");
        assertThat(body).contains("Content-Type: application/pdf");
        assertThat(body).contains("receipt-content");
    }

    @Test
    void testExtractText_MalformedJson() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("{invalid-json}"));

        byte[] bytes = "receipt-content".getBytes();
        assertThatThrownBy(() -> provider.extractText(bytes, "image/png"))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("Invalid JSON response");
    }

    @Test
    void testExtractText_EmptyResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(""));

        byte[] bytes = "receipt-content".getBytes();
        assertThatThrownBy(() -> provider.extractText(bytes, "image/png"))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("PaddleOCR response is null or empty");
    }

    @Test
    void testExtractText_MissingText() {
        String jsonResponse = """
                {
                  "confidence": 0.95,
                  "language": "en"
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        byte[] bytes = "receipt-content".getBytes();
        assertThatThrownBy(() -> provider.extractText(bytes, "image/png"))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("PaddleOCR response is missing required 'text' field");
    }

    @Test
    void testExtractText_InvalidConfidence() {
        String jsonResponse = """
                {
                  "text": "Hello",
                  "confidence": -0.5
                }
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        byte[] bytes = "receipt-content".getBytes();
        assertThatThrownBy(() -> provider.extractText(bytes, "image/png"))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("PaddleOCR response contains invalid confidence value");
    }

    @Test
    void testExtractText_Http500_RetryFailure() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Server error 1"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Server error 2"));
        mockWebServer.enqueue(new MockResponse().setResponseCode(500).setBody("Server error 3"));

        byte[] bytes = "receipt-content".getBytes();
        assertThatThrownBy(() -> provider.extractText(bytes, "image/png"))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("Internal Server Error");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
    }

    @Test
    void testExtractText_Http404_NoRetry() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(404).setBody("Not found"));

        byte[] bytes = "receipt-content".getBytes();
        assertThatThrownBy(() -> provider.extractText(bytes, "image/png"))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("Not Found");

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void testExtractText_RetrySucceeds() throws Exception {
        mockWebServer.enqueue(new MockResponse().setResponseCode(503).setBody("Service unavailable"));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "text": "Recovered text",
                          "confidence": 0.88
                        }
                        """));

        byte[] bytes = "receipt-content".getBytes();
        OcrResult result = provider.extractText(bytes, "image/png");

        assertThat(result).isNotNull();
        assertThat(result.getText()).isEqualTo("Recovered text");
        assertThat(result.getConfidence()).isEqualTo(0.88);
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    @Test
    void testExtractText_Timeout_RetriesAndFails() throws Exception {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory();
        requestFactory.setReadTimeout(Duration.ofMillis(50));
        
        RestClient timeoutRestClient = RestClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .requestFactory(requestFactory)
                .build();
        
        PaddleOcrProvider timeoutProvider = new PaddleOcrProvider(timeoutRestClient);
        PaddleOcrProvider.initialBackoffMs = 1;

        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));
        mockWebServer.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        byte[] bytes = "receipt-content".getBytes();
        assertThatThrownBy(() -> timeoutProvider.extractText(bytes, "image/png"))
                .isInstanceOf(IngestionException.class)
                .hasMessageContaining("Connection failure or timeout when calling PaddleOCR");
    }

    @Test
    void testMultipartRequestStructureAndContract() throws Exception {
        String jsonResponse = """
                {
                  "text": "Hello World",
                  "confidence": 0.95
                }
                """;
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(jsonResponse));

        byte[] bytes = "sample-image-data".getBytes();
        provider.extractText(bytes, "image/png");

        RecordedRequest request = mockWebServer.takeRequest();
        
        // 1. Verify HTTP method and path
        assertThat(request.getPath()).isEqualTo("/ocr");
        assertThat(request.getMethod()).isEqualTo("POST");

        // 2. Verify Content-Type uses multipart/form-data
        assertThat(request.getHeader("Content-Type")).contains("multipart/form-data");

        String body = request.getBody().readUtf8();

        // 3. Verify multipart contains a part named "file"
        assertThat(body).contains("name=\"file\"");

        // 4. Verify filename is preserved
        assertThat(body).contains("filename=\"document.png\"");

        // 5. Verify MIME type is preserved
        assertThat(body).contains("Content-Type: image/png");

        // 6. Verify payload contents are preserved
        assertThat(body).contains("sample-image-data");
    }
}
