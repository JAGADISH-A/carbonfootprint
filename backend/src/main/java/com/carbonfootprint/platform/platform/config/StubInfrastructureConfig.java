package com.carbonfootprint.platform.platform.config;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.document.port.out.RawDocumentRepository;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.ingestion.port.out.DocumentParser;
import com.carbonfootprint.platform.integration.ocr.OcrProvider;
import com.carbonfootprint.platform.integration.ocr.StubOcrProvider;
import com.carbonfootprint.platform.integration.ocr.model.OcrResult;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Configuration
@Profile("stub")
public class StubInfrastructureConfig {

    public StubInfrastructureConfig() {
        log.info("StubInfrastructureConfig active. Stubs enabled for OCR, Gemini, and Firestore.");
    }

    @Bean
    public OcrProvider stubOcrProvider() {
        return new StubOcrProvider();
    }

    @Bean
    public DocumentParser stubDocumentParser() {
        return new DocumentParser() {
            @Override
            public boolean supports(RawDocument document) {
                return true; // Stub supports everything
            }

            @Override
            public ExtractionResult parse(RawDocument document) {
                log.info("StubDocumentParser parsing document (stubbed).");
                return ExtractionResult.builder()
                        .parserName("StubDocumentParser")
                        .category(ActivityCategory.OTHER)
                        .description("Parsed by Stub")
                        .confidence(1.0)
                        .metadata(Map.of())
                        .build();
            }
        };
    }

    @Bean
    public RawDocumentRepository stubRawDocumentRepository() {
        return new RawDocumentRepository() {
            private final Map<String, RawDocument> store = new ConcurrentHashMap<>();

            @Override
            public RawDocument save(RawDocument document) {
                log.info("StubRawDocumentRepository saving RawDocument id={}", document.getId());
                store.put(document.getId(), document);
                return document;
            }

            @Override
            public Optional<RawDocument> findById(String id) {
                return Optional.ofNullable(store.get(id));
            }
        };
    }

    @Bean
    public ActivityRepository stubActivityRepository() {
        return new ActivityRepository() {
            private final Map<String, Activity> store = new ConcurrentHashMap<>();

            @Override
            public Activity save(Activity activity) {
                log.info("StubActivityRepository saving Activity id={}", activity.getId());
                store.put(activity.getId(), activity);
                return activity;
            }

            @Override
            public Optional<Activity> findById(String id) {
                return Optional.ofNullable(store.get(id));
            }

            @Override
            public List<Activity> findByUserId(String userId) {
                return store.values().stream().filter(a -> userId.equals(a.getUserId())).toList();
            }

            @Override
            public List<Activity> findByUserIdAndOccurredAtBetween(String userId, Instant from, Instant to) {
                return store.values().stream()
                        .filter(a -> userId.equals(a.getUserId()))
                        .filter(a -> a.getOccurredAt() != null && !a.getOccurredAt().isBefore(from) && !a.getOccurredAt().isAfter(to))
                        .toList();
            }

            @Override
            public boolean existsByUserIdAndRawDocumentId(String userId, String rawDocumentId) {
                return store.values().stream()
                        .anyMatch(a -> userId.equals(a.getUserId()) && rawDocumentId.equals(a.getRawDocumentId()));
            }

            @Override
            public void deleteById(String id) {
                store.remove(id);
            }
        };
    }
}
