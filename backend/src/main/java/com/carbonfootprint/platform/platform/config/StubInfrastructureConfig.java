package com.carbonfootprint.platform.platform.config;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactorRegistry;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.document.port.out.RawDocumentRepository;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.ingestion.port.out.DocumentParser;
import com.carbonfootprint.platform.integration.ocr.OcrProvider;
import com.carbonfootprint.platform.integration.ocr.StubOcrProvider;
import com.carbonfootprint.platform.integration.ocr.model.OcrResult;
import com.carbonfootprint.platform.mobile.model.Device;
import com.carbonfootprint.platform.mobile.model.PairingCode;
import com.carbonfootprint.platform.mobile.repository.DeviceRepository;
import com.carbonfootprint.platform.mobile.repository.PairingCodeRepository;
import com.carbonfootprint.platform.platform.exception.IngestionException;
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
            public boolean existsByMobileTransaction(String userId, String deviceId, String transactionId) {
                return store.values().stream()
                        .filter(a -> userId.equals(a.getUserId()))
                        .anyMatch(a -> {
                            Object meta = a.getMetadata();
                            if (meta instanceof Map<?, ?> m) {
                                return transactionId.equals(m.get("transactionId"))
                                        && deviceId.equals(m.get("deviceId"));
                            }
                            return false;
                        });
            }

            @Override
            public void deleteById(String id) {
                store.remove(id);
            }
        };
    }

    @Bean
    public DeviceRepository stubDeviceRepository() {
        return new DeviceRepository() {
            private final Map<String, Device> store = new ConcurrentHashMap<>();

            @Override
            public Device save(Device device) {
                store.put(device.getId(), device);
                return device;
            }

            @Override
            public Optional<Device> findById(String id) {
                return Optional.ofNullable(store.get(id));
            }

            @Override
            public Optional<Device> findByDeviceId(String deviceId) {
                return store.values().stream()
                        .filter(d -> deviceId.equals(d.getDeviceId()))
                        .findFirst();
            }

            @Override
            public Optional<Device> findByRefreshTokenHash(String refreshTokenHash) {
                return store.values().stream()
                        .filter(d -> refreshTokenHash.equals(d.getRefreshTokenHash()))
                        .findFirst();
            }

            @Override
            public List<Device> findByUserId(String userId) {
                return store.values().stream()
                        .filter(d -> userId.equals(d.getUserId()))
                        .toList();
            }

            @Override
            public void deleteById(String id) {
                store.remove(id);
            }
        };
    }

    @Bean
    public PairingCodeRepository stubPairingCodeRepository() {
        return new PairingCodeRepository() {
            private final Map<String, PairingCode> store = new ConcurrentHashMap<>();

            @Override
            public PairingCode save(PairingCode pairingCode) {
                store.put(pairingCode.getCode(), pairingCode);
                return pairingCode;
            }

            @Override
            public Optional<PairingCode> findByCode(String code) {
                return Optional.ofNullable(store.get(code));
            }

            @Override
            public void deleteByCode(String code) {
                store.remove(code);
            }
        };
    }

    @Bean
    public EmissionFactorRegistry stubEmissionFactorRegistry() {
        List<EmissionFactor> factors = List.of(
                // ── FUEL ──────────────────────────────────────────────
                EmissionFactor.builder()
                        .id("stub-fuel-petrol").category(ActivityCategory.FUEL)
                        .fuelType(FuelType.PETROL)
                        .value(new java.math.BigDecimal("2.31")).unit("litre")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),
                EmissionFactor.builder()
                        .id("stub-fuel-diesel").category(ActivityCategory.FUEL)
                        .fuelType(FuelType.DIESEL)
                        .value(new java.math.BigDecimal("2.68")).unit("litre")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),
                EmissionFactor.builder()
                        .id("stub-fuel-lpg").category(ActivityCategory.FUEL)
                        .fuelType(FuelType.LPG)
                        .value(new java.math.BigDecimal("1.51")).unit("litre")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),
                EmissionFactor.builder()
                        .id("stub-fuel-cng").category(ActivityCategory.FUEL)
                        .fuelType(FuelType.CNG)
                        .value(new java.math.BigDecimal("2.50")).unit("kg")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),

                // ── ELECTRICITY ───────────────────────────────────────
                EmissionFactor.builder()
                        .id("stub-electricity-grid").category(ActivityCategory.ELECTRICITY)
                        .value(new java.math.BigDecimal("0.82")).unit("kWh")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),

                // ── FLIGHT ────────────────────────────────────────────
                EmissionFactor.builder()
                        .id("stub-flight-economy").category(ActivityCategory.FLIGHT)
                        .value(new java.math.BigDecimal("0.14")).unit("passenger-km")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),
                EmissionFactor.builder()
                        .id("stub-flight-business").category(ActivityCategory.FLIGHT)
                        .value(new java.math.BigDecimal("0.42")).unit("passenger-km")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),

                // ── TRANSPORT ─────────────────────────────────────────
                EmissionFactor.builder()
                        .id("stub-transport-taxi").category(ActivityCategory.TRANSPORT)
                        .transportMode(TransportMode.TAXI)
                        .value(new java.math.BigDecimal("0.21")).unit("km")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),
                EmissionFactor.builder()
                        .id("stub-transport-bus").category(ActivityCategory.TRANSPORT)
                        .transportMode(TransportMode.BUS)
                        .value(new java.math.BigDecimal("0.089")).unit("km")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),
                EmissionFactor.builder()
                        .id("stub-transport-metro").category(ActivityCategory.TRANSPORT)
                        .transportMode(TransportMode.METRO)
                        .value(new java.math.BigDecimal("0.05")).unit("km")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),
                EmissionFactor.builder()
                        .id("stub-transport-train").category(ActivityCategory.TRANSPORT)
                        .transportMode(TransportMode.TRAIN)
                        .value(new java.math.BigDecimal("0.04")).unit("km")
                        .source("STUB").version("stub-v1").methodName("Stub — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build(),

                // ── SHOPPING ──────────────────────────────────────────
                EmissionFactor.builder()
                        .id("stub-shopping-default").category(ActivityCategory.SHOPPING)
                        .value(new java.math.BigDecimal("0.00085")).unit("INR")
                        .source("STUB").version("stub-v1").methodName("Stub — Spend-based EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z")).build()
        );

        return new EmissionFactorRegistry() {
            private final List<EmissionFactor> allFactors = List.copyOf(factors);

            @Override
            public Optional<EmissionFactor> find(ActivityCategory category, Instant validAt) {
                return allFactors.stream()
                        .filter(f -> f.getCategory() == category)
                        .filter(f -> f.isValidAt(validAt))
                        .filter(f -> f.getFuelType().isEmpty())
                        .filter(f -> f.getTransportMode().isEmpty())
                        .min(java.util.Comparator.comparing(EmissionFactor::getValidFrom).reversed());
            }

            @Override
            public Optional<EmissionFactor> find(ActivityCategory category, FuelType fuelType, Instant validAt) {
                return allFactors.stream()
                        .filter(f -> f.getCategory() == category)
                        .filter(f -> f.getFuelType().orElse(null) == fuelType)
                        .filter(f -> f.isValidAt(validAt))
                        .min(java.util.Comparator.comparing(EmissionFactor::getValidFrom).reversed());
            }

            @Override
            public Optional<EmissionFactor> find(ActivityCategory category, TransportMode transportMode, Instant validAt) {
                return allFactors.stream()
                        .filter(f -> f.getCategory() == category)
                        .filter(f -> f.getTransportMode().orElse(null) == transportMode)
                        .filter(f -> f.isValidAt(validAt))
                        .min(java.util.Comparator.comparing(EmissionFactor::getValidFrom).reversed());
            }

            @Override
            public Optional<EmissionFactor> find(ActivityCategory category, FuelType fuelType,
                                                  TransportMode transportMode, String region, Instant validAt) {
                return allFactors.stream()
                        .filter(f -> f.getCategory() == category)
                        .filter(f -> f.getFuelType().orElse(null) == fuelType || f.getFuelType().isEmpty())
                        .filter(f -> f.getTransportMode().orElse(null) == transportMode || f.getTransportMode().isEmpty())
                        .filter(f -> f.isValidAt(validAt))
                        .min(java.util.Comparator.comparing(EmissionFactor::getValidFrom).reversed());
            }

            @Override
            public List<EmissionFactor> findAll() { return allFactors; }

            @Override
            public List<EmissionFactor> findByCategory(ActivityCategory category) {
                return allFactors.stream().filter(f -> f.getCategory() == category).toList();
            }

            @Override
            public int count() { return allFactors.size(); }
        };
    }
}
