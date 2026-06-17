package com.carbonfootprint.platform.integration.ocr.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import lombok.extern.slf4j.Slf4j;
import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Configuration
@Profile("!stub")
@EnableConfigurationProperties(OcrProperties.class)
public class OcrClientConfiguration {

    @Bean
    public RestClient ocrRestClient(OcrProperties ocrProperties, RestClient.Builder restClientBuilder) {
        OcrProperties.Paddle paddle = ocrProperties.getPaddle();
        String baseUrl = paddle.getBaseUrl();
        int timeoutSeconds = paddle.getTimeoutSeconds();
        log.info("Configured OCR Base URL = {}", baseUrl);

        // Enforce HTTP/1.1 to prevent h2c upgrade failures.
        // By default, Java 21 HttpClient attempts HTTP/2 cleartext (h2c) upgrades.
        // Uvicorn's default HTTP parser rejects h2c upgrade requests with a bad request error 
        // before routing to FastAPI, so HTTP/1.1 is forced to ensure stable interoperability.
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        return restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
