package com.carbonfootprint.platform.integration.ocr.paddle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaddleOcrResponse {
    private String text;
    private Double confidence;
    private String language;
    private Integer pageCount;
}
