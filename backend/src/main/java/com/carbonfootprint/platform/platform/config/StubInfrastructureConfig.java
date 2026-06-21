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
import com.carbonfootprint.platform.mobile.model.Device;
import com.carbonfootprint.platform.mobile.model.PairingCode;
import com.carbonfootprint.platform.mobile.repository.DeviceRepository;
import com.carbonfootprint.platform.mobile.repository.PairingCodeRepository;
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
        log.info("StubInfrastructureConfig active. Stubs enabled for OCR, Groq, and Firestore.");
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
            {
                seedActivities();
            }

            private void seedActivities() {
                Instant now = Instant.now();
                String userId = com.carbonfootprint.platform.shared.constant.DemoUser.ID;

                addSeed(userId, ActivityCategory.ELECTRICITY, "Tata Power", "Monthly electricity bill", new java.math.BigDecimal("2500"), "INR", now.minus(java.time.Duration.ofDays(1)), new java.math.BigDecimal("82.5"));
                addSeed(userId, ActivityCategory.TRANSPORT, "Delhi Metro", "Metro commute", new java.math.BigDecimal("80"), "INR", now.minus(java.time.Duration.ofDays(2)), new java.math.BigDecimal("4.2"));
                addSeed(userId, ActivityCategory.FOOD, "Barbeque Nation", "Dinner with family", new java.math.BigDecimal("3200"), "INR", now.minus(java.time.Duration.ofDays(3)), new java.math.BigDecimal("8.5"));
                addSeed(userId, ActivityCategory.FUEL, "Shell Fuel Station", "Petrol refill 20L", new java.math.BigDecimal("2100"), "INR", now.minus(java.time.Duration.ofDays(5)), new java.math.BigDecimal("46.2"));
                addSeed(userId, ActivityCategory.SHOPPING, "Zara", "Summer clothes purchase", new java.math.BigDecimal("4500"), "INR", now.minus(java.time.Duration.ofDays(7)), new java.math.BigDecimal("12.0"));
                addSeed(userId, ActivityCategory.TRANSPORT, "Uber", "Rideshare to airport", new java.math.BigDecimal("650"), "INR", now.minus(java.time.Duration.ofDays(9)), new java.math.BigDecimal("10.5"));
                addSeed(userId, ActivityCategory.FOOD, "Starbucks", "Coffee & snacks", new java.math.BigDecimal("450"), "INR", now.minus(java.time.Duration.ofDays(11)), new java.math.BigDecimal("3.1"));
                addSeed(userId, ActivityCategory.ELECTRICITY, "Tata Power", "Weekly smart meter sync", new java.math.BigDecimal("0"), "INR", now.minus(java.time.Duration.ofDays(15)), new java.math.BigDecimal("75.0"));
                addSeed(userId, ActivityCategory.WATER, "Municipal Water", "Water utility charge", new java.math.BigDecimal("350"), "INR", now.minus(java.time.Duration.ofDays(18)), new java.math.BigDecimal("1.8"));
                addSeed(userId, ActivityCategory.SHOPPING, "Amazon", "New office chair", new java.math.BigDecimal("7200"), "INR", now.minus(java.time.Duration.ofDays(20)), new java.math.BigDecimal("24.5"));
                addSeed(userId, ActivityCategory.FOOD, "Reliance Smart", "Weekly grocery run", new java.math.BigDecimal("2800"), "INR", now.minus(java.time.Duration.ofDays(22)), new java.math.BigDecimal("15.2"));
                addSeed(userId, ActivityCategory.TRANSPORT, "Indian Railways", "Train to office", new java.math.BigDecimal("150"), "INR", now.minus(java.time.Duration.ofDays(24)), new java.math.BigDecimal("3.5"));
                addSeed(userId, ActivityCategory.FUEL, "BPCL Station", "Petrol refill 23L", new java.math.BigDecimal("2400"), "INR", now.minus(java.time.Duration.ofDays(26)), new java.math.BigDecimal("52.8"));
                addSeed(userId, ActivityCategory.FOOD, "Zomato", "Lunch ordering", new java.math.BigDecimal("620"), "INR", now.minus(java.time.Duration.ofDays(27)), new java.math.BigDecimal("5.6"));
                addSeed(userId, ActivityCategory.FLIGHT, "IndiGo Airlines", "Domestic flight BOM-DEL", new java.math.BigDecimal("5500"), "INR", now.minus(java.time.Duration.ofDays(28)), new java.math.BigDecimal("280.0"));
            }

            private void addSeed(String userId, ActivityCategory category, String merchant, String description,
                                 java.math.BigDecimal amount, String currency, Instant occurredAt, java.math.BigDecimal carbonKg) {
                String id = java.util.UUID.randomUUID().toString();
                java.util.Map<String, Object> metadata = new java.util.HashMap<>();
                java.util.Map<String, Object> assessment = new java.util.HashMap<>();
                assessment.put("carbonKg", carbonKg);
                assessment.put("methodology", "EMISSION_FACTOR");
                metadata.put("carbonAssessment", assessment);

                Activity activity = Activity.builder()
                        .id(id)
                        .schemaVersion(1)
                        .userId(userId)
                        .source(com.carbonfootprint.platform.activity.model.ActivitySource.MANUAL)
                        .category(category)
                        .merchant(merchant)
                        .amount(amount)
                        .currency(currency)
                        .occurredAt(occurredAt)
                        .description(description)
                        .metadata(metadata)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();

                store.put(id, activity);
            }

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

    @Bean
    public com.carbonfootprint.platform.mobile.port.out.PendingActivityRepository stubPendingActivityRepository() {
        return new com.carbonfootprint.platform.mobile.port.out.PendingActivityRepository() {
            private final Map<String, com.carbonfootprint.platform.mobile.model.PendingActivity> store = new ConcurrentHashMap<>();

            @Override
            public com.carbonfootprint.platform.mobile.model.PendingActivity save(com.carbonfootprint.platform.mobile.model.PendingActivity pendingActivity) {
                store.put(pendingActivity.getId(), pendingActivity);
                return pendingActivity;
            }

            @Override
            public Optional<com.carbonfootprint.platform.mobile.model.PendingActivity> findById(String id) {
                return Optional.ofNullable(store.get(id));
            }

            @Override
            public boolean exists(String id) {
                return store.containsKey(id);
            }

            @Override
            public void updateStatus(String id, com.carbonfootprint.platform.mobile.model.PendingActivityStatus status) {
                com.carbonfootprint.platform.mobile.model.PendingActivity existing = store.get(id);
                if (existing != null) {
                    store.put(id, existing.toBuilder()
                            .status(status)
                            .processingStartedAt(status == com.carbonfootprint.platform.mobile.model.PendingActivityStatus.PROCESSING ? Instant.now() : existing.getProcessingStartedAt())
                            .processedAt(status == com.carbonfootprint.platform.mobile.model.PendingActivityStatus.PROCESSED ? Instant.now() : existing.getProcessedAt())
                            .build());
                }
            }

            @Override
            public void updateFailure(String id, String errorMessage) {
                com.carbonfootprint.platform.mobile.model.PendingActivity existing = store.get(id);
                if (existing != null) {
                    store.put(id, existing.toBuilder()
                            .status(com.carbonfootprint.platform.mobile.model.PendingActivityStatus.FAILED)
                            .retryCount(existing.getRetryCount() + 1)
                            .lastError(errorMessage)
                            .lastRetryAt(Instant.now())
                            .build());
                }
            }

            @Override
            public com.carbonfootprint.platform.mobile.model.PendingActivity upsert(com.carbonfootprint.platform.mobile.model.PendingActivity pendingActivity) {
                com.carbonfootprint.platform.mobile.model.PendingActivity existing = store.get(pendingActivity.getId());
                if (existing != null) {
                    com.carbonfootprint.platform.mobile.model.PendingActivity.PendingActivityBuilder builder = existing.toBuilder()
                            .syncSessionId(pendingActivity.getSyncSessionId())
                            .rawPayload(pendingActivity.getRawPayload())
                            .merchant(pendingActivity.getMerchant())
                            .amount(pendingActivity.getAmount())
                            .timestamp(pendingActivity.getTimestamp());
                    if (existing.getStatus() == null || existing.getStatus() == com.carbonfootprint.platform.mobile.model.PendingActivityStatus.FAILED) {
                        builder.status(com.carbonfootprint.platform.mobile.model.PendingActivityStatus.NEW)
                               .retryCount(0)
                               .lastError(null);
                    }
                    com.carbonfootprint.platform.mobile.model.PendingActivity updated = builder.build();
                    store.put(pendingActivity.getId(), updated);
                    return updated;
                } else {
                    store.put(pendingActivity.getId(), pendingActivity);
                    return pendingActivity;
                }
            }

            @Override
            public long countPendingByDeviceId(String deviceId) {
                return store.values().stream()
                        .filter(p -> deviceId.equals(p.getDeviceId()))
                        .filter(p -> p.getStatus() != com.carbonfootprint.platform.mobile.model.PendingActivityStatus.PROCESSED)
                        .count();
            }

            @Override
            public java.util.List<com.carbonfootprint.platform.mobile.model.PendingActivity> findPendingByDeviceId(String deviceId) {
                return store.values().stream()
                        .filter(p -> deviceId.equals(p.getDeviceId()))
                        .filter(p -> p.getStatus() != com.carbonfootprint.platform.mobile.model.PendingActivityStatus.PROCESSED)
                        .toList();
            }
        };

    }


    @Bean
    public com.carbonfootprint.platform.integration.ai.groq.GroqDocumentParser stubGroqDocumentParser(
            com.fasterxml.jackson.databind.ObjectMapper objectMapper
    ) {
        return new com.carbonfootprint.platform.integration.ai.groq.GroqDocumentParser(null, null, null, objectMapper) {
            @Override
            public boolean supports(com.carbonfootprint.platform.document.model.RawDocument document) {
                return true;
            }

            @Override
            public com.carbonfootprint.platform.ingestion.model.ExtractionResult parse(com.carbonfootprint.platform.document.model.RawDocument document) {
                log.info("StubGroqDocumentParser parsing document (stubbed).");
                return com.carbonfootprint.platform.ingestion.model.ExtractionResult.builder()
                        .parserName("StubGroqDocumentParser")
                        .category(com.carbonfootprint.platform.activity.model.ActivityCategory.OTHER)
                        .description("Parsed by Stub")
                        .confidence(1.0)
                        .metadata(Map.of())
                        .build();
            }
        };
    }
}
