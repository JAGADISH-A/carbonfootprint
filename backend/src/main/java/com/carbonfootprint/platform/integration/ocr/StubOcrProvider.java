package com.carbonfootprint.platform.integration.ocr;

import com.carbonfootprint.platform.integration.ocr.model.OcrResult;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class StubOcrProvider implements OcrProvider {

    @Override
    public String getProviderName() {
        return "StubOCR";
    }

    @Override
    public OcrResult extractText(byte[] fileBytes, String mimeType) throws IngestionException {
        log.info("StubOCR extracting text (stubbed).");
        return OcrResult.builder()
                .text("Stubbed OCR Text")
                .confidence(1.0)
                .language("en")
                .metadata(Map.of("engineName", "StubOCR"))
                .build();
    }
}
