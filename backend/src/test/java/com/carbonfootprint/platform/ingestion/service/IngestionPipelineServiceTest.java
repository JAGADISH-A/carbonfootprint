package com.carbonfootprint.platform.ingestion.service;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.activity.model.ActivitySource;
import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.carbonfootprint.platform.carbon.calculation.CarbonCalculationEngine;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionResult;
import com.carbonfootprint.platform.document.model.RawDocument;
import com.carbonfootprint.platform.document.port.out.RawDocumentRepository;
import com.carbonfootprint.platform.ingestion.model.ExtractionResult;
import com.carbonfootprint.platform.ingestion.model.IngestionRequest;
import com.carbonfootprint.platform.ingestion.model.IngestionResult;
import com.carbonfootprint.platform.ingestion.model.ValidationResult;
import com.carbonfootprint.platform.ingestion.normalization.ActivityCarbonEnricher;
import com.carbonfootprint.platform.ingestion.normalization.ActivityNormalizer;
import com.carbonfootprint.platform.ingestion.normalization.ExtractionResultNormalizer;
import com.carbonfootprint.platform.ingestion.normalization.ExtractionResultToActivityConverter;
import com.carbonfootprint.platform.ingestion.port.out.DocumentParser;
import com.carbonfootprint.platform.ingestion.port.out.IngestionSource;
import com.carbonfootprint.platform.ingestion.validation.ExtractionResultValidator;
import com.carbonfootprint.platform.ingestion.validation.RawDocumentValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IngestionPipelineServiceTest {

    @Mock private IngestionSource ingestionSource;
    @Mock private RawDocumentValidator rawDocumentValidator;
    @Mock private DocumentParser documentParser;
    @Mock private ExtractionResultNormalizer extractionResultNormalizer;
    @Mock private ActivityCarbonEnricher activityCarbonEnricher;
    @Mock private ExtractionResultValidator extractionResultValidator;
    @Mock private ExtractionResultToActivityConverter activityConverter;
    @Mock private ActivityNormalizer activityNormalizer;
    @Mock private CarbonCalculationEngine carbonCalculationEngine;
    @Mock private RawDocumentRepository rawDocumentRepository;
    @Mock private ActivityRepository activityRepository;

    private IngestionPipelineService pipelineService;

    @BeforeEach
    void setUp() {
        pipelineService = new IngestionPipelineService(
                List.of(ingestionSource),
                List.of(rawDocumentValidator),
                List.of(documentParser),
                extractionResultNormalizer,
                activityCarbonEnricher,
                List.of(extractionResultValidator),
                activityConverter,
                List.of(activityNormalizer),
                carbonCalculationEngine,
                rawDocumentRepository,
                activityRepository
        );
    }

    private IngestionRequest sampleRequest() {
        return IngestionRequest.builder()
                .userId("user-001")
                .source(ActivitySource.RECEIPT)
                .mimeType("image/jpeg")
                .fileBytes(new byte[]{1, 2, 3})
                .build();
    }

    private RawDocument sampleRawDocument() {
        return RawDocument.builder()
                .id("doc-001")
                .source(ActivitySource.RECEIPT)
                .mimeType("image/jpeg")
                .build();
    }

    private ExtractionResult sampleExtractionResult() {
        return ExtractionResult.builder()
                .parserName("test-parser")
                .confidence(0.85)
                .merchant("Shell")
                .amount(BigDecimal.valueOf(2000))
                .currency("INR")
                .category(ActivityCategory.FUEL)
                .metadata(Map.of("carbonHints", Map.of("fuelType", "PETROL")))
                .build();
    }

    private Activity sampleActivity() {
        return Activity.builder()
                .id("act-001")
                .category(ActivityCategory.FUEL)
                .merchant("Shell")
                .amount(BigDecimal.valueOf(40))
                .unit("litres")
                .metadata(Map.of("carbonHints", Map.of("fuelType", "PETROL")))
                .build();
    }

    private EmissionResult sampleEmissionResult() {
        return EmissionResult.builder()
                .carbonKg(BigDecimal.valueOf(92.4))
                .activityId("act-001")
                .emissionFactor(EmissionFactor.builder()
                        .id("ef-petrol-001")
                        .value(BigDecimal.valueOf(2.31))
                        .unit("litre")
                        .category(ActivityCategory.FUEL)
                        .source("DEFRA 2024")
                        .version("2024-v1")
                        .methodName("Tier 1 — Default EF")
                        .validFrom(Instant.parse("2024-01-01T00:00:00Z"))
                        .build())
                .activityQuantity(BigDecimal.valueOf(40))
                .activityUnit("litre")
                .methodology("Tier 1 — Default EF")
                .calculatedAt(Instant.parse("2026-01-15T10:00:00Z"))
                .build();
    }

    private void setupHappyPath() {
        when(ingestionSource.supports(any())).thenReturn(true);
        when(ingestionSource.ingest(any())).thenReturn(sampleRawDocument());
        when(rawDocumentRepository.save(any())).thenReturn(sampleRawDocument());
        when(rawDocumentValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        when(documentParser.supports(any())).thenReturn(true);
        when(documentParser.parse(any())).thenReturn(sampleExtractionResult());
        when(extractionResultNormalizer.normalize(any())).thenReturn(sampleExtractionResult());
        when(activityCarbonEnricher.enrich(any())).thenReturn(sampleExtractionResult());
        when(extractionResultValidator.validate(any())).thenReturn(ValidationResult.ok());
        when(activityConverter.convert(any(), any(), any())).thenReturn(sampleActivity());
        when(activityNormalizer.normalize(any())).thenReturn(sampleActivity());
        when(activityRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ── CarbonCalculationEngine integration ────────────────────────────────

    @Test
    void ingest_callsCarbonCalculationEngine() {
        setupHappyPath();
        when(carbonCalculationEngine.calculate(any())).thenReturn(Optional.of(sampleEmissionResult()));

        pipelineService.ingest(sampleRequest());

        verify(carbonCalculationEngine).calculate(any(Activity.class));
    }

    @Test
    void ingest_storesCarbonAssessmentInMetadata() {
        setupHappyPath();
        when(carbonCalculationEngine.calculate(any())).thenReturn(Optional.of(sampleEmissionResult()));

        pipelineService.ingest(sampleRequest());

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());

        Activity savedActivity = captor.getValue();
        assertThat(savedActivity.getMetadata()).containsKey("carbonAssessment");

        @SuppressWarnings("unchecked")
        Map<String, Object> assessment = (Map<String, Object>) savedActivity.getMetadata().get("carbonAssessment");
        assertThat(assessment).containsEntry("carbonKg", BigDecimal.valueOf(92.4));
        assertThat(assessment).containsEntry("methodology", "Tier 1 — Default EF");
        assertThat(assessment).containsEntry("emissionFactorValue", BigDecimal.valueOf(2.31));
        assertThat(assessment).containsEntry("emissionFactorSource", "DEFRA 2024");
        assertThat(assessment).containsEntry("emissionFactorUnit", "litre");
        assertThat(assessment).containsEntry("activityQuantity", BigDecimal.valueOf(40));
        assertThat(assessment).containsKey("calculatedAt");
    }

    @Test
    void ingest_whenCalculationReturnsEmpty_storesNoAssessment() {
        setupHappyPath();
        when(carbonCalculationEngine.calculate(any())).thenReturn(Optional.empty());

        pipelineService.ingest(sampleRequest());

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());

        Activity savedActivity = captor.getValue();
        assertThat(savedActivity.getMetadata()).doesNotContainKey("carbonAssessment");
    }

    @Test
    void ingest_whenCalculationThrowsException_continuesPipeline() {
        setupHappyPath();
        when(carbonCalculationEngine.calculate(any())).thenThrow(new RuntimeException("Calculation error"));

        IngestionResult result = pipelineService.ingest(sampleRequest());

        assertThat(result.isSuccess()).isTrue();
        verify(activityRepository).save(any());
    }

    @Test
    void ingest_preservesExistingMetadataDuringCarbonCalculation() {
        setupHappyPath();
        Activity activityWithMetadata = sampleActivity().withMetadata(
                Map.of("carbonHints", Map.of("fuelType", "PETROL"), "existingKey", "existingValue")
        );
        when(activityNormalizer.normalize(any())).thenReturn(activityWithMetadata);
        when(carbonCalculationEngine.calculate(any())).thenReturn(Optional.of(sampleEmissionResult()));

        pipelineService.ingest(sampleRequest());

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityRepository).save(captor.capture());

        Activity savedActivity = captor.getValue();
        assertThat(savedActivity.getMetadata()).containsKey("existingKey");
        assertThat(savedActivity.getMetadata()).containsKey("carbonAssessment");
    }

    // ── Pipeline still works without carbon result ─────────────────────────

    @Test
    void ingest_withoutCarbonResult_persistsActivityNormally() {
        setupHappyPath();
        when(carbonCalculationEngine.calculate(any())).thenReturn(Optional.empty());

        IngestionResult result = pipelineService.ingest(sampleRequest());

        assertThat(result.isSuccess()).isTrue();
        verify(activityRepository).save(any());
    }

    // ── Pipeline ordering ──────────────────────────────────────────────────

    @Test
    void ingest_carbonCalculationRunsAfterNormalization() {
        setupHappyPath();
        when(carbonCalculationEngine.calculate(any())).thenReturn(Optional.of(sampleEmissionResult()));

        pipelineService.ingest(sampleRequest());

        var normalizerOrder = inOrder(activityNormalizer, carbonCalculationEngine);
        normalizerOrder.verify(activityNormalizer).normalize(any());
        normalizerOrder.verify(carbonCalculationEngine).calculate(any());
    }

    @Test
    void ingest_persistsActivityAfterCarbonCalculation() {
        setupHappyPath();
        when(carbonCalculationEngine.calculate(any())).thenReturn(Optional.of(sampleEmissionResult()));

        pipelineService.ingest(sampleRequest());

        var order = inOrder(carbonCalculationEngine, activityRepository);
        order.verify(carbonCalculationEngine).calculate(any());
        order.verify(activityRepository).save(any());
    }
}
