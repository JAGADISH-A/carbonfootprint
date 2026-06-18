package com.carbonfootprint.platform.carbon.analytics.service;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.activity.port.out.ActivityRepository;
import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.analytics.model.CategoryEmissionSummary;
import com.carbonfootprint.platform.carbon.analytics.model.MonthlyEmissionTrend;
import com.carbonfootprint.platform.carbon.analytics.model.TopEmissionActivity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarbonAnalyticsServiceTest {

    @Mock
    private ActivityRepository activityRepository;

    private CarbonAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new CarbonAnalyticsService(activityRepository);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Activity activityWithAssessment(
            String id, ActivityCategory category, String merchant,
            BigDecimal carbonKg, Instant occurredAt) {

        Map<String, Object> assessment = Map.of(
                "carbonKg", carbonKg,
                "methodology", "Tier 1 — Default EF"
        );

        return Activity.builder()
                .id(id)
                .category(category)
                .merchant(merchant)
                .amount(BigDecimal.valueOf(100))
                .unit("kg")
                .occurredAt(occurredAt)
                .metadata(Map.of("carbonAssessment", assessment))
                .build();
    }

    private Activity activityWithoutAssessment(String id) {
        return Activity.builder()
                .id(id)
                .category(ActivityCategory.OTHER)
                .metadata(Map.of())
                .build();
    }

    // ── Empty / no data ────────────────────────────────────────────────────

    @Test
    void getAnalytics_noActivities_returnsEmpty() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of());

        Optional<CarbonAnalyticsResponse> result = service.getAnalytics("user-001");

        assertThat(result).isEmpty();
    }

    @Test
    void getAnalytics_activitiesWithoutAssessment_returnsEmpty() {
        when(activityRepository.findByUserId("user-001")).thenReturn(
                List.of(activityWithoutAssessment("act-001"), activityWithoutAssessment("act-002"))
        );

        Optional<CarbonAnalyticsResponse> result = service.getAnalytics("user-001");

        assertThat(result).isEmpty();
    }

    @Test
    void getAnalytics_mixedActivities_returnsOnlyAssessed() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(10), Instant.parse("2026-01-15T10:00:00Z")),
                activityWithoutAssessment("act-002"),
                activityWithAssessment("act-003", ActivityCategory.ELECTRICITY, "BSES", BigDecimal.valueOf(20), Instant.parse("2026-01-20T10:00:00Z"))
        ));

        Optional<CarbonAnalyticsResponse> result = service.getAnalytics("user-001");

        assertThat(result).isPresent();
        assertThat(result.get().getActivityCount()).isEqualTo(2);
        assertThat(result.get().getTotalCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(30));
    }

    // ── Total emissions ────────────────────────────────────────────────────

    @Test
    void getAnalytics_multipleActivities_sumsCorrectly() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(10), Instant.parse("2026-01-15T10:00:00Z")),
                activityWithAssessment("act-002", ActivityCategory.FUEL, "BP", BigDecimal.valueOf(25), Instant.parse("2026-01-16T10:00:00Z")),
                activityWithAssessment("act-003", ActivityCategory.TRANSPORT, "Uber", BigDecimal.valueOf(5), Instant.parse("2026-01-17T10:00:00Z"))
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(40));
        assertThat(response.getActivityCount()).isEqualTo(3);
    }

    // ── Category-wise totals ───────────────────────────────────────────────

    @Test
    void getAnalytics_categoryTotals_groupsByCategory() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(10), Instant.parse("2026-01-15T10:00:00Z")),
                activityWithAssessment("act-002", ActivityCategory.FUEL, "BP", BigDecimal.valueOf(20), Instant.parse("2026-01-16T10:00:00Z")),
                activityWithAssessment("act-003", ActivityCategory.ELECTRICITY, "BSES", BigDecimal.valueOf(30), Instant.parse("2026-01-17T10:00:00Z"))
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getCategoryTotals()).hasSize(2);

        CategoryEmissionSummary fuelSummary = response.getCategoryTotals().stream()
                .filter(c -> c.getCategory().equals("FUEL"))
                .findFirst().orElseThrow();
        assertThat(fuelSummary.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(fuelSummary.getActivityCount()).isEqualTo(2);
        assertThat(fuelSummary.getPercentageOfTotal()).isEqualByComparingTo(BigDecimal.valueOf(50.0));

        CategoryEmissionSummary elecSummary = response.getCategoryTotals().stream()
                .filter(c -> c.getCategory().equals("ELECTRICITY"))
                .findFirst().orElseThrow();
        assertThat(elecSummary.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(elecSummary.getActivityCount()).isEqualTo(1);
        assertThat(elecSummary.getPercentageOfTotal()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
    }

    @Test
    void getAnalytics_categoryTotals_sortedByCarbonKgDescending() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(10), Instant.parse("2026-01-15T10:00:00Z")),
                activityWithAssessment("act-002", ActivityCategory.ELECTRICITY, "BSES", BigDecimal.valueOf(50), Instant.parse("2026-01-16T10:00:00Z")),
                activityWithAssessment("act-003", ActivityCategory.TRANSPORT, "Uber", BigDecimal.valueOf(5), Instant.parse("2026-01-17T10:00:00Z"))
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getCategoryTotals()).hasSize(3);
        assertThat(response.getCategoryTotals().get(0).getCategory()).isEqualTo("ELECTRICITY");
        assertThat(response.getCategoryTotals().get(1).getCategory()).isEqualTo("FUEL");
        assertThat(response.getCategoryTotals().get(2).getCategory()).isEqualTo("TRANSPORT");
    }

    // ── Monthly trend ──────────────────────────────────────────────────────

    @Test
    void getAnalytics_monthlyTrend_groupsByMonth() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(10), Instant.parse("2026-01-15T10:00:00Z")),
                activityWithAssessment("act-002", ActivityCategory.FUEL, "BP", BigDecimal.valueOf(20), Instant.parse("2026-01-20T10:00:00Z")),
                activityWithAssessment("act-003", ActivityCategory.ELECTRICITY, "BSES", BigDecimal.valueOf(30), Instant.parse("2026-02-10T10:00:00Z"))
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getMonthlyTrend()).hasSize(2);

        MonthlyEmissionTrend jan = response.getMonthlyTrend().stream()
                .filter(m -> m.getMonth().equals("2026-01"))
                .findFirst().orElseThrow();
        assertThat(jan.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(jan.getActivityCount()).isEqualTo(2);

        MonthlyEmissionTrend feb = response.getMonthlyTrend().stream()
                .filter(m -> m.getMonth().equals("2026-02"))
                .findFirst().orElseThrow();
        assertThat(feb.getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(30));
        assertThat(feb.getActivityCount()).isEqualTo(1);
    }

    @Test
    void getAnalytics_monthlyTrend_sortedByMonth() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(10), Instant.parse("2026-03-15T10:00:00Z")),
                activityWithAssessment("act-002", ActivityCategory.FUEL, "BP", BigDecimal.valueOf(20), Instant.parse("2026-01-20T10:00:00Z")),
                activityWithAssessment("act-003", ActivityCategory.ELECTRICITY, "BSES", BigDecimal.valueOf(30), Instant.parse("2026-02-10T10:00:00Z"))
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getMonthlyTrend()).hasSize(3);
        assertThat(response.getMonthlyTrend().get(0).getMonth()).isEqualTo("2026-01");
        assertThat(response.getMonthlyTrend().get(1).getMonth()).isEqualTo("2026-02");
        assertThat(response.getMonthlyTrend().get(2).getMonth()).isEqualTo("2026-03");
    }

    // ── Top 5 activities ───────────────────────────────────────────────────

    @Test
    void getAnalytics_topActivities_returnsTop5ByCarbonKg() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(50), Instant.parse("2026-01-15T10:00:00Z")),
                activityWithAssessment("act-002", ActivityCategory.FUEL, "BP", BigDecimal.valueOf(100), Instant.parse("2026-01-16T10:00:00Z")),
                activityWithAssessment("act-003", ActivityCategory.TRANSPORT, "Uber", BigDecimal.valueOf(10), Instant.parse("2026-01-17T10:00:00Z")),
                activityWithAssessment("act-004", ActivityCategory.ELECTRICITY, "BSES", BigDecimal.valueOf(200), Instant.parse("2026-01-18T10:00:00Z")),
                activityWithAssessment("act-005", ActivityCategory.SHOPPING, "Amazon", BigDecimal.valueOf(75), Instant.parse("2026-01-19T10:00:00Z")),
                activityWithAssessment("act-006", ActivityCategory.FLIGHT, "IndiGo", BigDecimal.valueOf(150), Instant.parse("2026-01-20T10:00:00Z")),
                activityWithAssessment("act-007", ActivityCategory.GAS, "Indane", BigDecimal.valueOf(5), Instant.parse("2026-01-21T10:00:00Z"))
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTopActivities()).hasSize(5);
        assertThat(response.getTopActivities().get(0).getActivityId()).isEqualTo("act-004");
        assertThat(response.getTopActivities().get(0).getCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(200));
        assertThat(response.getTopActivities().get(1).getActivityId()).isEqualTo("act-006");
        assertThat(response.getTopActivities().get(2).getActivityId()).isEqualTo("act-002");
        assertThat(response.getTopActivities().get(3).getActivityId()).isEqualTo("act-005");
        assertThat(response.getTopActivities().get(4).getActivityId()).isEqualTo("act-001");
    }

    @Test
    void getAnalytics_topActivities_limitedTo5() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(50), Instant.parse("2026-01-15T10:00:00Z")),
                activityWithAssessment("act-002", ActivityCategory.FUEL, "BP", BigDecimal.valueOf(100), Instant.parse("2026-01-16T10:00:00Z"))
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTopActivities()).hasSize(2);
    }

    @Test
    void getAnalytics_topActivities_includesMethodology() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(50), Instant.parse("2026-01-15T10:00:00Z"))
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        TopEmissionActivity top = response.getTopActivities().get(0);
        assertThat(top.getMethodology()).isEqualTo("Tier 1 — Default EF");
        assertThat(top.getMerchant()).isEqualTo("Shell");
        assertThat(top.getCategory()).isEqualTo("FUEL");
    }

    // ── Average daily emission ─────────────────────────────────────────────

    @Test
    void getAnalytics_withDateRange_calculatesAverageDaily() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-11T00:00:00Z"); // 10 days

        when(activityRepository.findByUserIdAndOccurredAtBetween("user-001", from, to))
                .thenReturn(List.of(
                        activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(100), Instant.parse("2026-01-05T10:00:00Z"))
                ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001", from, to).orElseThrow();

        assertThat(response.getAverageDailyKg()).isEqualByComparingTo(BigDecimal.valueOf(10.0000));
        assertThat(response.getPeriodStart()).isEqualTo(from);
        assertThat(response.getPeriodEnd()).isEqualTo(to);
    }

    @Test
    void getAnalytics_withoutDateRange_averageDailyIsZero() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(100), Instant.parse("2026-01-05T10:00:00Z"))
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getAverageDailyKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getAnalytics_dateRange_noPeriod_returnsZero() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-01T00:00:00Z"); // same day = 0 days

        when(activityRepository.findByUserIdAndOccurredAtBetween("user-001", from, to))
                .thenReturn(List.of(
                        activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(100), from)
                ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001", from, to).orElseThrow();

        assertThat(response.getAverageDailyKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ── Edge cases ─────────────────────────────────────────────────────────

    @Test
    void getActivities_withNullOccurredAt_excludedFromMonthlyTrend() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.FUEL)
                        .merchant("Shell")
                        .occurredAt(null)
                        .metadata(Map.of("carbonAssessment", Map.of("carbonKg", BigDecimal.valueOf(10))))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getActivityCount()).isEqualTo(1);
        assertThat(response.getMonthlyTrend()).isEmpty();
    }

    @Test
    void getAnalytics_withNumberCarbonKg_extractsCorrectly() {
        Map<String, Object> assessment = Map.of(
                "carbonKg", 42.5,
                "methodology", "Tier 1"
        );

        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.FUEL)
                        .merchant("Shell")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", assessment))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(42.5));
    }

    @Test
    void getAnalytics_dateRange_filtersCorrectly() {
        Instant from = Instant.parse("2026-01-01T00:00:00Z");
        Instant to = Instant.parse("2026-01-31T23:59:59Z");

        when(activityRepository.findByUserIdAndOccurredAtBetween("user-001", from, to))
                .thenReturn(List.of(
                        activityWithAssessment("act-001", ActivityCategory.FUEL, "Shell", BigDecimal.valueOf(10), Instant.parse("2026-01-15T10:00:00Z"))
                ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001", from, to).orElseThrow();

        assertThat(response.getActivityCount()).isEqualTo(1);
        assertThat(response.getPeriodStart()).isEqualTo(from);
        assertThat(response.getPeriodEnd()).isEqualTo(to);
    }

    @Test
    void getAnalytics_categoryWithNullCategory_treatedAsOther() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(null)
                        .merchant("Unknown")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", Map.of("carbonKg", BigDecimal.valueOf(10))))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getCategoryTotals()).hasSize(1);
        assertThat(response.getCategoryTotals().get(0).getCategory()).isEqualTo("OTHER");
    }

    // ── carbonKg type coercion ───────────────────────────────────────────

    @Test
    void getAnalytics_stringCarbonKg_parsesCorrectly() {
        Map<String, Object> assessment = Map.of(
                "carbonKg", "1.168750",
                "methodology", "Tier 1"
        );

        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.FUEL)
                        .merchant("Shell")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", assessment))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(new BigDecimal("1.168750"));
    }

    @Test
    void getAnalytics_stringCarbonKg_integerString_parsesCorrectly() {
        Map<String, Object> assessment = Map.of(
                "carbonKg", "42",
                "methodology", "Tier 1"
        );

        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.FUEL)
                        .merchant("Shell")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", assessment))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(42));
    }

    @Test
    void getAnalytics_doubleCarbonKg_parsesCorrectly() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.ELECTRICITY)
                        .merchant("BSES")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", Map.of(
                                "carbonKg", 3.14159,
                                "methodology", "Tier 1"
                        )))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(3.14159));
    }

    @Test
    void getAnalytics_bigDecimalCarbonKg_parsesCorrectly() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.FUEL)
                        .merchant("Shell")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", Map.of(
                                "carbonKg", new BigDecimal("99.999"),
                                "methodology", "Tier 1"
                        )))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(new BigDecimal("99.999"));
    }

    @Test
    void getAnalytics_missingCarbonKg_returnsZero() {
        Map<String, Object> assessment = Map.of(
                "methodology", "Tier 1"
        );

        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.FUEL)
                        .merchant("Shell")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", assessment))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getAnalytics_invalidStringCarbonKg_returnsZero() {
        Map<String, Object> assessment = Map.of(
                "carbonKg", "not-a-number",
                "methodology", "Tier 1"
        );

        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.FUEL)
                        .merchant("Shell")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", assessment))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getAnalytics_integerCarbonKg_parsesCorrectly() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.TRANSPORT)
                        .merchant("Uber")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", Map.of(
                                "carbonKg", 7,
                                "methodology", "Tier 1"
                        )))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(7));
    }

    @Test
    void getAnalytics_longCarbonKg_parsesCorrectly() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.TRANSPORT)
                        .merchant("Uber")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", Map.of(
                                "carbonKg", 100L,
                                "methodology", "Tier 1"
                        )))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void getAnalytics_mixedCarbonKgTypes_sumsCorrectly() {
        when(activityRepository.findByUserId("user-001")).thenReturn(List.of(
                Activity.builder()
                        .id("act-001")
                        .category(ActivityCategory.FUEL)
                        .merchant("Shell")
                        .occurredAt(Instant.parse("2026-01-15T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", Map.of(
                                "carbonKg", "10.5",
                                "methodology", "Tier 1"
                        )))
                        .build(),
                Activity.builder()
                        .id("act-002")
                        .category(ActivityCategory.ELECTRICITY)
                        .merchant("BSES")
                        .occurredAt(Instant.parse("2026-01-16T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", Map.of(
                                "carbonKg", 20.0,
                                "methodology", "Tier 1"
                        )))
                        .build(),
                Activity.builder()
                        .id("act-003")
                        .category(ActivityCategory.TRANSPORT)
                        .merchant("Uber")
                        .occurredAt(Instant.parse("2026-01-17T10:00:00Z"))
                        .metadata(Map.of("carbonAssessment", Map.of(
                                "carbonKg", new BigDecimal("30"),
                                "methodology", "Tier 1"
                        )))
                        .build()
        ));

        CarbonAnalyticsResponse response = service.getAnalytics("user-001").orElseThrow();

        assertThat(response.getTotalCarbonKg()).isEqualByComparingTo(new BigDecimal("60.5"));
        assertThat(response.getActivityCount()).isEqualTo(3);
    }
}
