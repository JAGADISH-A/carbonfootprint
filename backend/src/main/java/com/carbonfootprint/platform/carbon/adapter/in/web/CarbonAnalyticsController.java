package com.carbonfootprint.platform.carbon.adapter.in.web;

import com.carbonfootprint.platform.carbon.analytics.model.CarbonAnalyticsResponse;
import com.carbonfootprint.platform.carbon.port.in.CarbonAnalyticsUseCase;
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
 * REST controller for carbon emission analytics.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /api/v1/carbon/analytics} — full analytics for the user</li>
 *   <li>{@code GET /api/v1/carbon/analytics?from=...&to=...} — filtered by date range</li>
 * </ul>
 *
 * <h3>Auth note</h3>
 * Currently uses a placeholder userId. Will be replaced with JWT subject claim
 * once Google OAuth2 is integrated.
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.CARBON_PATH)
@RequiredArgsConstructor
@Tag(name = "Carbon Analytics", description = "Read-only carbon emission analytics endpoints")
public class CarbonAnalyticsController {

    private final CarbonAnalyticsUseCase carbonAnalyticsUseCase;

    @GetMapping("/analytics")
    @Operation(summary = "Get aggregated carbon emission analytics for the authenticated user")
    public ResponseEntity<ApiResponse<CarbonAnalyticsResponse>> getAnalytics(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        String userId = getCurrentUserId();
        log.debug("Carbon analytics request: userId={} from={} to={}", userId, from, to);

        Optional<CarbonAnalyticsResponse> response;

        if (from != null && to != null) {
            Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusNanos(1);
            response = carbonAnalyticsUseCase.getAnalytics(userId, fromInstant, toInstant);
        } else {
            response = carbonAnalyticsUseCase.getAnalytics(userId);
        }

        return response
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(
                        CarbonAnalyticsResponse.builder()
                                .totalCarbonKg(java.math.BigDecimal.ZERO)
                                .activityCount(0)
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
        return "anonymous";
    }
}
