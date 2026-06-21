package com.carbonfootprint.platform.carbon.adapter.in.web;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonInsightResponse;
import com.carbonfootprint.platform.carbon.port.in.CarbonInsightUseCase;
import com.carbonfootprint.platform.platform.web.ApiResponse;
import com.carbonfootprint.platform.shared.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * REST controller for deterministic carbon emission insights.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /api/v1/carbon/insights} — full insights for the user</li>
 *   <li>{@code GET /api/v1/carbon/insights?from=...&to=...} — filtered by date range</li>
 * </ul>
 *
 * <h3>Flow</h3>
 * Controller → {@link CarbonInsightUseCase} → {@code CarbonInsightService}
 *     → {@link com.carbonfootprint.platform.carbon.port.in.CarbonAnalyticsUseCase} → Firestore
 *
 * <h3>Design</h3>
 * Thin controller — delegates all logic to the use case.
 * Reuses the existing analytics pipeline; no aggregation logic duplicated.
 *
 * <h3>Auth note</h3>
 * Currently uses a placeholder userId. Will be replaced with JWT subject claim
 * once Google OAuth2 is integrated.
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.CARBON_PATH)
@RequiredArgsConstructor
@Tag(name = "Carbon Insights", description = "Deterministic carbon emission insight endpoints")
public class CarbonInsightController {

    private final CarbonInsightUseCase carbonInsightUseCase;

    @GetMapping("/insights")
    @Operation(summary = "Get deterministic carbon emission insights for the authenticated user")
    public ResponseEntity<ApiResponse<CarbonInsightResponse>> getInsights(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        String userId = getCurrentUserId();
        log.debug("Carbon insights request: userId={} from={} to={}", userId, from, to);

        Optional<CarbonInsightResponse> response;

        if (from != null && to != null) {
            Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
            response = carbonInsightUseCase.generateInsights(userId, fromInstant, toInstant);
        } else {
            response = carbonInsightUseCase.generateInsights(userId);
        }

        return response
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(
                        CarbonInsightResponse.builder()
                                .summary("No carbon data found")
                                .achievements(java.util.List.of())
                                .warnings(java.util.List.of())
                                .recommendations(java.util.List.of())
                                .insights(java.util.List.of())
                                .build(),
                        "No carbon data found"
                )));
    }

    /**
     * Extracts the current user's identifier from the security context.
     *
     * @return the user ID, or "anonymous" as a placeholder
     */
    private String getCurrentUserId() {
        // TODO: Extract from SecurityContext after Google Auth integration
        return com.carbonfootprint.platform.shared.constant.DemoUser.ID;
    }
}
