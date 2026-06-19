package com.carbonfootprint.platform.carbon.coach.service;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import com.carbonfootprint.platform.carbon.coach.prompt.CarbonCoachPromptBuilder;
import com.carbonfootprint.platform.carbon.port.in.CarbonInsightUseCase;
import com.carbonfootprint.platform.integration.ai.groq.GroqClient;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AICarbonCoachServiceTest {

    @Mock
    private CarbonInsightUseCase carbonInsightUseCase;

    @Mock
    private GroqClient groqClient;

    @Mock
    private CarbonCoachPromptBuilder carbonCoachPromptBuilder;

    @Mock
    private CoachCacheService coachCacheService;

    @Mock
    private CoachConfidenceCalculator coachConfidenceCalculator;

    private AICarbonCoachService service;

    private static final String USER_ID = "user-001";
    private static final String SYSTEM_PROMPT = "You are EcoBuddy.";
    private static final String COACH_MODEL = "llama-3.3-70b-versatile";

    @BeforeEach
    void setUp() {
        service = new AICarbonCoachService(
                carbonInsightUseCase, groqClient, carbonCoachPromptBuilder,
                coachCacheService, coachConfidenceCalculator, COACH_MODEL);
        lenient().when(coachConfidenceCalculator.calculate(any())).thenReturn(75);
    }

    // ── Collaboration verification ────────────────────────────────────────

    @Test
    void generateCoach_invokesCarbonInsightUseCase() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn(validJson());

        service.generateCoach(USER_ID);

        verify(carbonInsightUseCase).generateInsights(USER_ID);
    }

    @Test
    void generateCoach_invokesCarbonCoachPromptBuilder() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn(validJson());

        service.generateCoach(USER_ID);

        verify(carbonCoachPromptBuilder).build(any(CarbonInsightResponse.class));
    }

    @Test
    void generateCoach_invokesGroqClientWithSystemMessage() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn(validJson());

        service.generateCoach(USER_ID);

        verify(groqClient).generateContent(eq(COACH_MODEL), eq(SYSTEM_PROMPT), eq("prompt"));
    }

    // ── Model selection ──────────────────────────────────────────────────

    @Test
    void generateCoach_usesCoachModel() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn(validJson());

        service.generateCoach(USER_ID);

        ArgumentCaptor<String> modelCaptor = ArgumentCaptor.forClass(String.class);
        verify(groqClient).generateContent(modelCaptor.capture(), eq(SYSTEM_PROMPT), eq("prompt"));
        assertThat(modelCaptor.getValue()).isEqualTo(COACH_MODEL);
    }

    @Test
    void generateCoach_withDateRange_invokesCarbonInsightUseCase() throws Exception {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        Instant fromInstant = Instant.parse("2026-01-01T00:00:00Z");
        Instant toInstant = Instant.parse("2026-02-01T00:00:00Z").minusNanos(1);

        when(carbonInsightUseCase.generateInsights(USER_ID, fromInstant, toInstant))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn(validJson());

        service.generateCoach(USER_ID, from, to);

        verify(carbonInsightUseCase).generateInsights(USER_ID, fromInstant, toInstant);
    }

    @Test
    void generateCoach_withDateRange_invokesGroqClientWithSystemMessage() throws Exception {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        Instant fromInstant = Instant.parse("2026-01-01T00:00:00Z");
        Instant toInstant = Instant.parse("2026-02-01T00:00:00Z").minusNanos(1);

        when(carbonInsightUseCase.generateInsights(USER_ID, fromInstant, toInstant))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn(validJson());

        service.generateCoach(USER_ID, from, to);

        verify(groqClient).generateContent(eq(COACH_MODEL), eq(SYSTEM_PROMPT), eq("prompt"));
    }

    // ── Empty / no data ───────────────────────────────────────────────────

    @Test
    void generateCoach_noDateRange_noData_returnsEmpty() {
        when(carbonInsightUseCase.generateInsights(USER_ID)).thenReturn(Optional.empty());

        Optional<AICarbonCoachResponse> result = service.generateCoach(USER_ID);

        assertThat(result).isEmpty();
        verify(carbonInsightUseCase).generateInsights(USER_ID);
    }

    @Test
    void generateCoach_withDateRange_noData_returnsEmpty() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        Instant fromInstant = Instant.parse("2026-01-01T00:00:00Z");
        Instant toInstant = Instant.parse("2026-02-01T00:00:00Z").minusNanos(1);

        when(carbonInsightUseCase.generateInsights(USER_ID, fromInstant, toInstant))
                .thenReturn(Optional.empty());

        Optional<AICarbonCoachResponse> result = service.generateCoach(USER_ID, from, to);

        assertThat(result).isEmpty();
    }

    // ── AI success ────────────────────────────────────────────────────────

    @Test
    void generateCoach_aiSuccess_setsAiGeneratedTrue() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn(validJson());

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.isAiGenerated()).isTrue();
    }

    @Test
    void generateCoach_aiSuccess_parsesAllFields() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn(validJson());

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.getSummary()).isEqualTo("AI coaching summary");
        assertThat(response.getStrengths()).containsExactly("Strength 1", "Strength 2");
        assertThat(response.getConcerns()).containsExactly("Concern 1");
        assertThat(response.getRecommendations()).containsExactly("Rec 1", "Rec 2", "Rec 3");
        assertThat(response.getWeeklyChallenge()).isEqualTo("Walk instead of driving once");
        assertThat(response.getMotivation()).isEqualTo("Keep up the great work!");
    }

    // ── Markdown wrapped JSON ─────────────────────────────────────────────

    @Test
    void generateCoach_markdownWrappedJson_parsesCorrectly() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn("""
                ```json
                {
                  "summary": "Fenced JSON response",
                  "strengths": ["S1"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M"
                }
                ```
                """);

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.getSummary()).isEqualTo("Fenced JSON response");
        assertThat(response.isAiGenerated()).isTrue();
    }

    // ── Malformed JSON ────────────────────────────────────────────────────

    @Test
    void generateCoach_malformedJson_fallsBackToDeterministic() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn("not valid json {{{");

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getSummary()).isEqualTo("Your total footprint is 45.00 kg CO₂e.");
    }

    // ── Missing fields ────────────────────────────────────────────────────

    @Test
    void generateCoach_missingFields_parsesWhatItCan() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn("""
                {
                  "summary": "Partial data",
                  "strengths": ["One strength"]
                }
                """);

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.getSummary()).isEqualTo("Partial data");
        assertThat(response.getStrengths()).containsExactly("One strength");
        assertThat(response.getConcerns()).isEmpty();
        assertThat(response.getRecommendations()).isEmpty();
        assertThat(response.isAiGenerated()).isTrue();
    }

    // ── Empty arrays ──────────────────────────────────────────────────────

    @Test
    void generateCoach_emptyArrays_returnsEmptyLists() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn("""
                {
                  "summary": "All empty lists",
                  "strengths": [],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "",
                  "motivation": ""
                }
                """);

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.getStrengths()).isEmpty();
        assertThat(response.getConcerns()).isEmpty();
        assertThat(response.getRecommendations()).isEmpty();
        assertThat(response.isAiGenerated()).isTrue();
    }

    // ── Invalid JSON ──────────────────────────────────────────────────────

    @Test
    void generateCoach_invalidJson_fallsBackToDeterministic() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn("{invalid json");

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getSummary()).isEqualTo("Your total footprint is 45.00 kg CO₂e.");
    }

    // ── Empty summary from AI ─────────────────────────────────────────────

    @Test
    void generateCoach_emptySummary_fallsBackToDeterministic() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn("""
                {
                  "summary": "",
                  "strengths": ["S1"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M"
                }
                """);

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getSummary()).isEqualTo("Your total footprint is 45.00 kg CO₂e.");
    }

    // ── AI exception ──────────────────────────────────────────────────────

    @Test
    void generateCoach_aiException_fallsBackToDeterministic() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any()))
                .thenThrow(new IngestionException("Groq API error"));

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getSummary()).isEqualTo("Your total footprint is 45.00 kg CO₂e.");
    }

    @Test
    void generateCoach_aiException_preservesAllFields() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any()))
                .thenThrow(new IngestionException("API error"));

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.getStrengths()).containsExactly("Achievement 1", "Achievement 2");
        assertThat(response.getConcerns()).containsExactly("Warning 1");
        assertThat(response.getRecommendations()).containsExactly("Rec 1", "Rec 2");
        assertThat(response.getWeeklyChallenge()).isNotBlank();
        assertThat(response.getMotivation()).isNotBlank();
    }

    // ── AI timeout ────────────────────────────────────────────────────────

    @Test
    void generateCoach_aiTimeout_fallsBackToDeterministic() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any()))
                .thenThrow(new IngestionException("Connection failure or timeout"));

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getSummary()).isEqualTo("Your total footprint is 45.00 kg CO₂e.");
    }

    // ── Quota exceeded ────────────────────────────────────────────────────

    @Test
    void generateCoach_quotaExceeded_fallsBackToDeterministic() throws Exception {
        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(sampleInsight()));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any()))
                .thenThrow(new IngestionException("Groq API returned HTTP error: 429"));

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.isAiGenerated()).isFalse();
        assertThat(response.getSummary()).isEqualTo("Your total footprint is 45.00 kg CO₂e.");
    }

    // ── Cache behaviour ───────────────────────────────────────────────────

    @Test
    void generateCoach_cacheHit_doesNotCallGroq() throws Exception {
        CarbonInsightResponse insight = sampleInsight();
        AICarbonCoachResponse cachedResponse = AICarbonCoachResponse.builder()
                .summary("Cached AI summary")
                .strengths(List.of("Cached strength"))
                .concerns(List.of())
                .recommendations(List.of())
                .weeklyChallenge("Cached challenge")
                .motivation("Cached motivation")
                .aiGenerated(true)
                .build();

        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(insight));
        when(coachCacheService.get(eq(USER_ID), any())).thenReturn(cachedResponse);

        AICarbonCoachResponse response = service.generateCoach(USER_ID).orElseThrow();

        assertThat(response.getSummary()).isEqualTo("Cached AI summary");
        assertThat(response.isAiGenerated()).isTrue();
        verify(groqClient, never()).generateContent(any(), any());
        verify(coachCacheService, never()).put(any(), any(), any());
    }

    @Test
    void generateCoach_cacheMiss_callsGroqAndStores() throws Exception {
        CarbonInsightResponse insight = sampleInsight();
        CarbonAnalyticsResponse analytics = insight.getAnalytics();

        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(insight));
        when(coachCacheService.get(eq(USER_ID), eq(analytics))).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn(validJson());

        service.generateCoach(USER_ID);

        verify(groqClient).generateContent(eq(COACH_MODEL), eq(SYSTEM_PROMPT), eq("prompt"));
        verify(coachCacheService).put(eq(USER_ID), eq(analytics), any());
    }

    @Test
    void generateCoach_aiFailure_doesNotCache() throws Exception {
        CarbonInsightResponse insight = sampleInsight();
        CarbonAnalyticsResponse analytics = insight.getAnalytics();

        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(insight));
        when(coachCacheService.get(eq(USER_ID), eq(analytics))).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any()))
                .thenThrow(new IngestionException("Groq API error"));

        service.generateCoach(USER_ID);

        verify(coachCacheService, never()).put(any(), any(), any());
    }

    @Test
    void generateCoach_emptySummaryFromAi_doesNotCache() throws Exception {
        CarbonInsightResponse insight = sampleInsight();
        CarbonAnalyticsResponse analytics = insight.getAnalytics();

        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(insight));
        when(coachCacheService.get(eq(USER_ID), eq(analytics))).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn("""
                {
                  "summary": "",
                  "strengths": ["S1"],
                  "concerns": [],
                  "recommendations": [],
                  "weeklyChallenge": "C",
                  "motivation": "M"
                }
                """);

        service.generateCoach(USER_ID);

        verify(coachCacheService, never()).put(any(), any(), any());
    }

    @Test
    void generateCoach_fallbackResponse_doesNotCache() throws Exception {
        CarbonInsightResponse insight = sampleInsight();
        CarbonAnalyticsResponse analytics = insight.getAnalytics();

        when(carbonInsightUseCase.generateInsights(USER_ID))
                .thenReturn(Optional.of(insight));
        when(coachCacheService.get(eq(USER_ID), eq(analytics))).thenReturn(null);
        when(carbonCoachPromptBuilder.getSystemPrompt()).thenReturn(SYSTEM_PROMPT);
        when(carbonCoachPromptBuilder.build(any())).thenReturn("prompt");
        when(groqClient.generateContent(any(), any(), any())).thenReturn("not valid json {{{");

        service.generateCoach(USER_ID);

        verify(coachCacheService, never()).put(any(), any(), any());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private CarbonInsightResponse sampleInsight() {
        return CarbonInsightResponse.builder()
                .analytics(CarbonAnalyticsResponse.builder()
                        .activityCount(5)
                        .totalCarbonKg(new BigDecimal("45.0"))
                        .averageDailyKg(new BigDecimal("6.4"))
                        .build())
                .summary("Your total footprint is 45.00 kg CO₂e.")
                .achievements(List.of("Achievement 1", "Achievement 2"))
                .warnings(List.of("Warning 1"))
                .recommendations(List.of("Rec 1", "Rec 2"))
                .insights(List.of("Insight 1"))
                .build();
    }

    private String validJson() {
        return """
                {
                  "summary": "AI coaching summary",
                  "strengths": ["Strength 1", "Strength 2"],
                  "concerns": ["Concern 1"],
                  "recommendations": ["Rec 1", "Rec 2", "Rec 3"],
                  "weeklyChallenge": "Walk instead of driving once",
                  "motivation": "Keep up the great work!"
                }
                """;
    }
}
