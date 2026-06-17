package com.carbonfootprint.platform.integration.ocr;

import com.carbonfootprint.platform.integration.ocr.model.OcrResult;
import com.carbonfootprint.platform.platform.exception.IngestionException;

/**
 * Abstraction for all OCR (Optical Character Recognition) providers.
 *
 * <h3>Design</h3>
 * The pipeline never depends on PaddleOCR directly. Any OCR engine
 * (PaddleOCR, Google Cloud Vision, AWS Textract, Tesseract) can be
 * plugged in by implementing this interface.
 *
 * <p>Selection: use {@code @Primary} on the preferred implementation or
 * {@code @Qualifier} if multiple providers are active simultaneously.
 *
 * <p>Implementations:
 * {@link com.carbonfootprint.platform.integration.ocr.paddle.PaddleOcrAdapter}
 */
public interface OcrProvider {

    /**
     * Returns a human-readable name for this OCR provider.
     * Used for logging and {@link com.carbonfootprint.platform.document.model.RawDocument} metadata.
     *
     * @return provider name (e.g., "PaddleOCR", "GoogleCloudVision")
     */
    String getProviderName();

    /**
     * Extracts text from the given binary image or PDF content.
     *
     * @param fileBytes the raw bytes of the document
     * @param mimeType  the MIME type of the document (e.g., "image/jpeg")
     * @return the OCR extraction result
     * @throws com.carbonfootprint.platform.platform.exception.IngestionException on extraction failure
     */
    OcrResult extractText(byte[] fileBytes, String mimeType) throws IngestionException;
}
