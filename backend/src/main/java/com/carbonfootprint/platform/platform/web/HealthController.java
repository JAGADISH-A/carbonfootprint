package com.carbonfootprint.platform.platform.web;

import com.carbonfootprint.platform.shared.constant.ApiConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Health check endpoint — publicly accessible, no authentication required.
 *
 * <p>Used by Google Cloud Run health checks, load balancers, and monitoring
 * dashboards to verify the service is alive and ready.
 *
 * <p>Endpoint: {@code GET /api/v1/health}
 */
@RestController
@RequestMapping(ApiConstants.HEALTH_PATH)
@Tag(name = "Health", description = "Platform health and readiness endpoints")
public class HealthController {

    private final Optional<BuildProperties> buildProperties;

    public HealthController(Optional<BuildProperties> buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping
    @Operation(
            summary = "Health check",
            description = "Returns the current status of the Carbon Intelligence Platform. "
                    + "No authentication required."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("service", "carbon-intelligence-platform");
        status.put("version", buildProperties.map(BuildProperties::getVersion).orElse("dev"));
        status.put("timestamp", Instant.now().toString());

        return ResponseEntity.ok(ApiResponse.success(status, "Platform is healthy."));
    }
}
