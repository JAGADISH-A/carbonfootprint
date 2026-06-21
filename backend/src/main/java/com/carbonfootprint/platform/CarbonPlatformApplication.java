package com.carbonfootprint.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Carbon Intelligence Platform.
 *
 * <p>Architecture: Hexagonal (Ports & Adapters) + Domain-Driven Design.
 *
 * <p>Pipeline:
 * IngestionSource → RawDocument → Validation → DocumentParser → Normalization → Activity → Repository
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class CarbonPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CarbonPlatformApplication.class, args);
    }
}
