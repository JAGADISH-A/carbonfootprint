package com.carbonfootprint.platform.integration.ocr.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrResult {
    private String text;
    private Double confidence;
    private String language;
    private Map<String, Object> metadata;
}
