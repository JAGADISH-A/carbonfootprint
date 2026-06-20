package com.carbonfootprint.platform.mobile.controller;

import com.carbonfootprint.platform.activity.model.Activity;
import com.carbonfootprint.platform.mobile.dto.EnrichedTransaction;
import com.carbonfootprint.platform.mobile.service.MobileIngestionService;
import com.carbonfootprint.platform.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling pre-enriched transactions from the Android Companion App.
 *
 * <p>Exposes endpoints to receive mobile-ingested data and forwards it to the
 * {@link MobileIngestionService} for normalisation, carbon calculation, and persistence.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mobile/transactions")
@RequiredArgsConstructor
@Tag(name = "Mobile Ingestion", description = "Endpoints for Android Companion App data ingestion")
public class MobileIngestionController {

    private final MobileIngestionService mobileIngestionService;

    @PostMapping
    @Operation(summary = "Submit a pre-enriched transaction from the mobile app")
    public ResponseEntity<ApiResponse<Activity>> submitTransaction(
            @RequestBody EnrichedTransaction transaction,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        
        // Extract userId from request attribute populated by DeviceTokenFilter
        String userId = (String) httpRequest.getAttribute("userId");
        if (userId == null) {
            userId = "anonymous"; // Fallback if filter didn't run
        }
        
        log.info("Received mobile transaction ingestion request for userId={}", userId);

        Activity savedActivity = mobileIngestionService.processTransaction(transaction, userId);

        return ResponseEntity.ok(ApiResponse.success(
                savedActivity, 
                "Mobile transaction ingested successfully"
        ));
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
