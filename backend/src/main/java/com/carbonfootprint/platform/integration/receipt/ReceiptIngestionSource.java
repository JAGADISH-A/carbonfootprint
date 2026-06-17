package com.carbonfootprint.platform.integration.receipt;

import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.ingestion.model.IngestionRequest;
import com.carbonfootprint.platform.ingestion.port.out.IngestionSource;
import com.carbonfootprint.platform.integration.ocr.OcrProvider;
import com.carbonfootprint.platform.integration.ocr.model.OcrResult;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import com.carbonfootprint.platform.shared.util.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Ingestion source for physical and digital receipts (Phase 1).
 *
 * <h3>Responsibility</h3>
 * Accepts uploaded file bytes (JPEG, PNG, PDF), passes them to the configured
 * {@link OcrProvider}, and wraps the extracted text in a {@link RawDocument}.
 *
 * <h3>Supported MIME types</h3>
 * image/jpeg, image/png, image/webp, application/pdf
 *
 * <h3>Note</h3>
 * This class does NOT parse, validate, or calculate. It only extracts raw text.
 *
 * <p>TODO (Phase 1): Wire real OCR provider when PaddleOCR sidecar is deployed.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReceiptIngestionSource implements IngestionSource {

    private static final Set<String> SUPPORTED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "application/pdf"
    );

    private final OcrProvider ocrProvider;

    @Override
    public ActivitySource getSource() {
        return ActivitySource.RECEIPT;
    }

    @Override
    public boolean supports(IngestionRequest request) {
        return ActivitySource.RECEIPT.equals(request.getSource())
                && request.getMimeType() != null
                && SUPPORTED_MIME_TYPES.contains(request.getMimeType().toLowerCase());
    }

    @Override
    public RawDocument ingest(IngestionRequest request) throws IngestionException {
        Instant now = Instant.now();
        String checksum = calculateSha256(request.getFileBytes());
        String documentId = IdGenerator.generate();

        log.info("ReceiptIngestionSource: filename='{}', mimeType='{}', checksum='{}', metadata created",
                request.getOriginalFilename(), request.getMimeType(), checksum);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("originalFilename", request.getOriginalFilename());
        metadata.put("fileSizeBytes", request.getFileBytes() != null ? request.getFileBytes().length : 0);
        metadata.put("uploadTimestamp", now.toString());
        metadata.put("checksumSha256", checksum);
        metadata.put("ocrProvider", ocrProvider.getProviderName());
        metadata.put("ocrStatus", "PENDING");

        log.info("OCR started");
        long startTime = System.currentTimeMillis();
        OcrResult ocrResult;
        try {
            ocrResult = ocrProvider.extractText(request.getFileBytes(), request.getMimeType());
            long duration = System.currentTimeMillis() - startTime;
            metadata.put("ocrStatus", "COMPLETED");
            metadata.put("ocrCompletedAt", Instant.now().toString());
            log.info("OCR completed");
            log.info("extraction duration in milliseconds: {}", duration);
        } catch (Exception e) {
            metadata.put("ocrStatus", "FAILED");
            metadata.put("ocrFailureReason", e.getMessage());
            throw new IngestionException("OCR extraction failed for document " + documentId, e);
        }

        return RawDocument.builder()
                .id(documentId)
                .schemaVersion(1)
                .source(ActivitySource.RECEIPT)
                .mimeType(request.getMimeType())
                .rawText(ocrResult.getText())
                .language(ocrResult.getLanguage())
                .confidence(ocrResult.getConfidence())
                .userId(request.getUserId())
                .metadata(metadata)
                .createdAt(now)
                .build();
    }

    private String calculateSha256(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
