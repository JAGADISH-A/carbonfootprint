package com.carbonfootprint.platform.integration.ocr.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "carbon.ocr")
public class OcrProperties {

    private String provider;

    @Valid
    private Paddle paddle = new Paddle();

    @Getter
    @Setter
    public static class Paddle {
        @NotBlank(message = "carbon.ocr.paddle.base-url is mandatory")
        private String baseUrl;

        @Min(value = 1, message = "carbon.ocr.paddle.timeout-seconds must be at least 1")
        private int timeoutSeconds = 30;
    }
}
