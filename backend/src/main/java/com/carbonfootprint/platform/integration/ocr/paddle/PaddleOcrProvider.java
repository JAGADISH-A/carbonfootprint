package com.carbonfootprint.platform.integration.ocr.paddle;

import com.carbonfootprint.platform.integration.ocr.OcrProvider;
import com.carbonfootprint.platform.integration.ocr.model.OcrResult;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Profile("!stub")
public class PaddleOcrProvider implements OcrProvider {

    private static final String PROVIDER_NAME = "PaddleOCR";
    private static final int MAX_ATTEMPTS = 3;
    static long initialBackoffMs = 1000;
    private static final double BACKOFF_MULTIPLIER = 2.0;

    private final RestClient restClient;

    public PaddleOcrProvider(RestClient ocrRestClient) {
        this.restClient = ocrRestClient;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public OcrResult extractText(byte[] fileBytes, String mimeType) throws IngestionException {
        // Resolve correlation ID: reuse MDC if present, otherwise generate a UUID
        String requestId = MDC.get("requestId");
        if (requestId == null) {
            requestId = MDC.get("traceId");
        }
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }

        log.info("OCR request started. Provider: {}, Request ID: {}, Filename: {}", PROVIDER_NAME, requestId, "N/A");
        long startTime = System.currentTimeMillis();

        String extension = getExtensionFromMimeType(mimeType);
        String filename = "document" + extension;

        // Use MultipartBodyBuilder instead of manual multipart construction.
        // FastAPI's Python multipart parser (python-multipart) is strict regarding header formatting
        // (e.g. boundary formatting, content-disposition syntax). MultipartBodyBuilder ensures
        // RFC-compliant multipart formatting and correct Spring integration, preventing HTTP 400 parsing errors.
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part(
                "file",
                new ByteArrayResource(fileBytes) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                })
                .contentType(MediaType.parseMediaType(mimeType));

        MultiValueMap<String, HttpEntity<?>> parts = builder.build();

        if (log.isDebugEnabled()) {
            log.debug("Initiating OCR request. MimeType: {}, Filename: {}, Size: {} bytes", mimeType, filename, fileBytes.length);
        }

        PaddleOcrResponse response = null;
        long elapsedTime = 0;
        Exception lastException = null;

        long backoffDelay = initialBackoffMs;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                response = restClient.post()
                        .uri("/ocr")
                        .body(parts)
                        .retrieve()
                        .body(PaddleOcrResponse.class);

                elapsedTime = System.currentTimeMillis() - startTime;
                lastException = null;
                break;
            } catch (HttpServerErrorException e) {
                lastException = e;
                if (attempt == MAX_ATTEMPTS) {
                    break;
                }
                log.warn("PaddleOCR request failed with 5xx error (status={}). Retrying attempt {}/{} after {} ms...",
                        e.getStatusCode(), attempt, MAX_ATTEMPTS, backoffDelay);
            } catch (ResourceAccessException e) {
                lastException = e;
                if (attempt == MAX_ATTEMPTS) {
                    break;
                }
                log.warn("PaddleOCR request encountered network error ({}). Retrying attempt {}/{} after {} ms...",
                        e.getMessage(), attempt, MAX_ATTEMPTS, backoffDelay);
            } catch (HttpClientErrorException e) {
                // Do not retry 4xx errors
                elapsedTime = System.currentTimeMillis() - startTime;
                handleHttpError(e, requestId, elapsedTime);
            } catch (RestClientResponseException e) {
                // General client/server response exceptions (if not caught by subclasses)
                if (e.getStatusCode().is5xxServerError()) {
                    lastException = e;
                    if (attempt == MAX_ATTEMPTS) {
                        break;
                    }
                    log.warn("PaddleOCR request failed with general 5xx error (status={}). Retrying attempt {}/{} after {} ms...",
                            e.getStatusCode(), attempt, MAX_ATTEMPTS, backoffDelay);
                } else {
                    elapsedTime = System.currentTimeMillis() - startTime;
                    handleHttpError(e, requestId, elapsedTime);
                }
            } catch (RestClientException e) {
                elapsedTime = System.currentTimeMillis() - startTime;
                Throwable cause = e.getCause();
                if (cause instanceof org.springframework.http.converter.HttpMessageConversionException
                        || cause instanceof com.fasterxml.jackson.core.JsonProcessingException) {
                    log.error("OCR request completed. Request ID: {}, Elapsed time: {} ms, Status: FAILURE, Error: Invalid JSON response",
                            requestId, elapsedTime);
                    throw new IngestionException("Invalid JSON response from PaddleOCR: " + cause.getMessage(), e);
                }
                log.error("OCR request completed. Request ID: {}, Elapsed time: {} ms, Status: FAILURE, Error: Client call failure",
                        requestId, elapsedTime);
                throw new IngestionException("Client failure when calling PaddleOCR: " + e.getMessage(), e);
            } catch (Exception e) {
                // Unexpected runtime exceptions - do not retry
                elapsedTime = System.currentTimeMillis() - startTime;
                log.error("OCR request completed. Request ID: {}, Elapsed time: {} ms, Status: FAILURE, Error: {}",
                        requestId, elapsedTime, e.getMessage());
                throw new IngestionException("Unexpected failure during PaddleOCR call: " + e.getMessage(), e);
            }

            try {
                Thread.sleep(backoffDelay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("OCR request completed. Request ID: {}, Elapsed time: {} ms, Status: FAILURE, Error: Thread interrupted",
                        requestId, System.currentTimeMillis() - startTime);
                throw new IngestionException("OCR execution interrupted during retry backoff", ie);
            }
            backoffDelay = (long) (backoffDelay * BACKOFF_MULTIPLIER);
        }

        if (lastException != null) {
            elapsedTime = System.currentTimeMillis() - startTime;
            if (lastException instanceof RestClientResponseException rcre) {
                handleHttpError(rcre, requestId, elapsedTime);
            } else {
                log.error("OCR request completed. Request ID: {}, Elapsed time: {} ms, Status: FAILURE, Error: Connection failure or timeout",
                        requestId, elapsedTime);
                throw new IngestionException("Connection failure or timeout when calling PaddleOCR: " + lastException.getMessage(), lastException);
            }
        }

        // Response validation
        if (response == null) {
            log.error("OCR request completed. Request ID: {}, Elapsed time: {} ms, Status: FAILURE, Error: Empty response body",
                    requestId, elapsedTime);
            throw new IngestionException("PaddleOCR response is null or empty");
        }

        if (response.getText() == null) {
            log.error("OCR request completed. Request ID: {}, Elapsed time: {} ms, Status: FAILURE, Error: Missing text field",
                    requestId, elapsedTime);
            throw new IngestionException("PaddleOCR response is missing required 'text' field");
        }

        Double confidence = response.getConfidence();
        if (confidence != null && (confidence < 0.0 || confidence > 1.0)) {
            log.error("OCR request completed. Request ID: {}, Elapsed time: {} ms, Status: FAILURE, Error: Invalid confidence value ({})",
                    requestId, elapsedTime, confidence);
            throw new IngestionException("PaddleOCR response contains invalid confidence value: " + confidence);
        }

        String text = response.getText();
        String language = response.getLanguage();

        log.info("OCR request completed. Request ID: {}, Elapsed time: {} ms, Characters extracted: {}, Average confidence: {}, Language: {}, Status: SUCCESS",
                requestId, elapsedTime, text.length(), confidence != null ? confidence : "N/A", language != null ? language : "N/A", "SUCCESS");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", PROVIDER_NAME);
        metadata.put("language", language);
        metadata.put("confidence", confidence);
        metadata.put("processingTimeMs", elapsedTime);
        metadata.put("requestId", requestId);
        if (response.getPageCount() != null) {
            metadata.put("pageCount", response.getPageCount());
        }

        return OcrResult.builder()
                .text(text)
                .confidence(confidence)
                .language(language)
                .metadata(metadata)
                .build();
    }

    private void handleHttpError(RestClientResponseException e, String requestId, long elapsedTime) throws IngestionException {
        String statusCodeStr = String.valueOf(e.getStatusCode().value());
        String statusText = e.getStatusText();
        String responseBody = e.getResponseBodyAsString();

        String safeResponseBody = (responseBody != null && responseBody.length() > 200)
                ? responseBody.substring(0, 200) + "..."
                : responseBody;

        String endpointUrl = "/ocr";
        String errorMsg = String.format("PaddleOCR service returned HTTP error %s (%s) for URL: %s. Response: %s",
                statusCodeStr, statusText, endpointUrl, safeResponseBody);

        log.error("OCR request completed. Request ID: {}, Elapsed time: {} ms, Status: FAILURE, Error: HTTP {}",
                requestId, elapsedTime, statusCodeStr);

        int statusCode = e.getStatusCode().value();
        switch (statusCode) {
            case 400:
                throw new IngestionException("Bad Request: " + errorMsg, e);
            case 404:
                throw new IngestionException("Not Found: " + errorMsg, e);
            case 413:
                throw new IngestionException("Payload Too Large: The file uploaded is too big for PaddleOCR. " + errorMsg, e);
            case 415:
                throw new IngestionException("Unsupported Media Type: The document format is not supported by PaddleOCR. " + errorMsg, e);
            case 500:
                throw new IngestionException("Internal Server Error: PaddleOCR sidecar failed internally. " + errorMsg, e);
            case 502:
            case 503:
            case 504:
                throw new IngestionException("Gateway Failure: " + errorMsg, e);
            default:
                throw new IngestionException("HTTP Error: " + errorMsg, e);
        }
    }

    private String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) {
            return "";
        }
        return switch (mimeType.toLowerCase()) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
    }
}
