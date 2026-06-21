package com.carbonfootprint.platform.carbon.adapter.in.web;

import com.carbonfootprint.platform.carbon.coach.model.AICarbonCoachResponse;
import com.carbonfootprint.platform.carbon.port.in.AICarbonCoachUseCase;
import com.carbonfootprint.platform.platform.web.ApiResponse;
import com.carbonfootprint.platform.shared.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;

/**
 * REST controller for AI-powered carbon emission coaching.
 *
 * <h3>Endpoints</h3>
 * <ul>
 *   <li>{@code GET /api/v1/carbon/coach} — full AI coaching for the user</li>
 *   <li>{@code GET /api/v1/carbon/coach?from=...&to=...} — filtered by date range</li>
 * </ul>
 *
 * <h3>Flow</h3>
 * Controller → {@link AICarbonCoachUseCase} → {@code AICarbonCoachService}
 *     → Groq (with deterministic fallback)
 *
 * <h3>Design</h3>
 * Thin controller — delegates all logic to the use case.
 * Groq errors are handled by the service; this endpoint always succeeds.
 *
 * <h3>Auth note</h3>
 * Currently uses a placeholder userId. Will be replaced with JWT subject claim
 * once Google OAuth2 is integrated.
 */
@Slf4j
@RestController
@RequestMapping(ApiConstants.CARBON_PATH)
@RequiredArgsConstructor
@Tag(name = "AI Carbon Coach", description = "AI-powered personalized carbon emission coaching")
public class AICarbonCoachController {

    private final AICarbonCoachUseCase aiCarbonCoachUseCase;

    @GetMapping("/coach")
    @Operation(summary = "Get AI-powered carbon emission coaching for the authenticated user")
    public ResponseEntity<ApiResponse<AICarbonCoachResponse>> getCoach(
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        String userId = getCurrentUserId();
        log.debug("AI carbon coach request: userId={} from={} to={}", userId, from, to);

        Optional<AICarbonCoachResponse> response;

        if (from != null && to != null) {
            response = aiCarbonCoachUseCase.generateCoach(userId, from, to);
        } else {
            response = aiCarbonCoachUseCase.generateCoach(userId);
        }

        return response
                .map(r -> ResponseEntity.ok(ApiResponse.success(r)))
                .orElseGet(() -> ResponseEntity.ok(ApiResponse.success(
                        AICarbonCoachResponse.builder()
                                .summary("No carbon data found for coaching.")
                                .strengths(java.util.List.of())
                                .concerns(java.util.List.of())
                                .recommendations(java.util.List.of())
                                .weeklyChallenge("No challenge available.")
                                .motivation("Start tracking your activities to receive coaching.")
                                .aiGenerated(false)
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
