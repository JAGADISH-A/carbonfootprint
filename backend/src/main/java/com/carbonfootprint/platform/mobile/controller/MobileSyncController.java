package com.carbonfootprint.platform.mobile.controller;

import com.carbonfootprint.platform.mobile.dto.*;
import com.carbonfootprint.platform.mobile.service.MobileSyncService;
import com.carbonfootprint.platform.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * REST controller defining the contract for Android Companion App synchronization.
 *
 * <p>This controller exposes endpoints for device registration, heartbeat, sync status,
 * configuration, and transaction synchronization. Currently, these are placeholder
 * implementations that return mocked responses to establish the API contract.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
@Tag(name = "Mobile Synchronization", description = "Endpoints for Android Companion App device management and data sync")
public class MobileSyncController {

    private final MobileSyncService mobileSyncService;

    @PostMapping("/sync")
    @Operation(
            summary = "Synchronize enriched transactions from the mobile device",
            description = "Receives a batch of EnrichedTransaction objects and processes them. Validates device ID, timestamps, currency, amount, and merchant fields.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MobileSyncRequest.class),
                            examples = @ExampleObject(
                                    name = "Sample Sync Request",
                                    value = "{\n" +
                                            "  \"deviceId\": \"dev_123456789\",\n" +
                                            "  \"syncId\": \"sync_987654321\",\n" +
                                            "  \"transactions\": [\n" +
                                            "    {\n" +
                                            "      \"transactionId\": \"txn_111\",\n" +
                                            "      \"merchant\": \"Starbucks\",\n" +
                                            "      \"amount\": 5.50,\n" +
                                            "      \"currency\": \"USD\",\n" +
                                            "      \"category\": \"FOOD\",\n" +
                                            "      \"occurredAt\": \"2023-10-25T10:15:30Z\"\n" +
                                            "    }\n" +
                                            "  ]\n" +
                                            "}"
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Synchronization successful",
                            content = @Content(schema = @Schema(implementation = MobileSyncResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Validation Error - Invalid fields in request (e.g., negative amount, missing merchant)"
                    )
            }
    )
    public ResponseEntity<ApiResponse<MobileSyncResponse>> syncTransactions(
            @Valid @RequestBody MobileSyncRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String userId = (String) httpRequest.getAttribute("userId");
        String deviceId = (String) httpRequest.getAttribute("deviceId");
        
        // Ensure the token's deviceId matches the payload's deviceId
        if (!deviceId.equals(request.getDeviceId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("403", "Device ID mismatch"));
        }

        log.info("Mobile sync received: deviceId={} syncId={} transactions={} userId={}",
                request.getDeviceId(), request.getSyncId(), request.getTransactions().size(), userId);

        MobileSyncResponse response = mobileSyncService.processSync(request, userId);

        return ResponseEntity.ok(ApiResponse.success(response, "Synchronization complete"));
    }

    @PostMapping("/device/heartbeat")
    @Operation(
            summary = "Update device status via heartbeat",
            description = "Sends a periodic heartbeat indicating the device is alive and its current status."
    )
    public ResponseEntity<ApiResponse<HeartbeatResponse>> heartbeat(
            @Valid @RequestBody HeartbeatRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String deviceId = (String) httpRequest.getAttribute("deviceId");
        
        if (!deviceId.equals(request.getDeviceId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("403", "Device ID mismatch"));
        }

        log.info("Received heartbeat from device: {}", request.getDeviceId());

        HeartbeatResponse response = HeartbeatResponse.builder()
                .deviceId(request.getDeviceId())
                .serverTime(Instant.now())
                .syncRequired(false)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Heartbeat processed"));
    }

    @GetMapping("/config")
    @Operation(
            summary = "Get mobile configuration",
            description = "Returns the latest app configuration, including required versions, sync intervals, and feature flags."
    )
    public ResponseEntity<ApiResponse<MobileConfigResponse>> getConfig() {

        log.info("Returning mobile configuration");

        MobileConfigResponse response = MobileConfigResponse.builder()
                .latestAppVersion("1.0.0")
                .forceUpdate(false)
                .syncIntervalSeconds(3600)
                .featureFlags(Map.of("enableSmsParsing", "true", "enableLocationTracking", "false"))
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Configuration fetched"));
    }

    @GetMapping("/status")
    @Operation(
            summary = "Get sync status",
            description = "Returns the current synchronization status for a specific device."
    )
    public ResponseEntity<ApiResponse<SyncStatusResponse>> getStatus(
            jakarta.servlet.http.HttpServletRequest httpRequest) {

        String deviceId = (String) httpRequest.getAttribute("deviceId");

        log.info("Checking sync status for device: {}", deviceId);

        SyncStatusResponse response = SyncStatusResponse.builder()
                .deviceId(deviceId)
                .lastSuccessfulSync(Instant.now().minusSeconds(86400))
                .pendingTransactionsCount(0)
                .isSyncInProgress(false)
                .build();

        return ResponseEntity.ok(ApiResponse.success(response, "Status fetched"));
    }
}
