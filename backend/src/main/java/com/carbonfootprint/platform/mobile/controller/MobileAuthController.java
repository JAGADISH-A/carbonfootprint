package com.carbonfootprint.platform.mobile.controller;

import com.carbonfootprint.platform.mobile.dto.DeviceRegistrationRequest;
import com.carbonfootprint.platform.mobile.dto.PairingCodeResponse;
import com.carbonfootprint.platform.mobile.dto.TokenRefreshRequest;
import com.carbonfootprint.platform.mobile.dto.TokenResponse;
import com.carbonfootprint.platform.mobile.service.PairingService;
import com.carbonfootprint.platform.platform.web.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
@Tag(name = "Mobile Authentication", description = "Endpoints for Android Companion App device pairing and token management")
public class MobileAuthController {

    private final PairingService pairingService;

    @PostMapping("/pairing/generate")
    @Operation(summary = "Generate a new pairing code (Called by Web App)")
    public ResponseEntity<ApiResponse<PairingCodeResponse>> generatePairingCode() {
        // In Phase 4.4, we stub the web user ID since web auth is not fully wired yet
        String webUserId = "web-user-123";
        
        PairingCodeResponse response = pairingService.generatePairingCode(webUserId);
        return ResponseEntity.ok(ApiResponse.success(response, "Pairing code generated"));
    }

    @PostMapping("/pair")
    @Operation(summary = "Pair a mobile device using a pairing code (Called by Android)")
    public ResponseEntity<ApiResponse<TokenResponse>> pairDevice(
            @Valid @RequestBody DeviceRegistrationRequest request) {
            
        log.info("Pairing device {}", request.getDeviceId());
        TokenResponse response = pairingService.pairDevice(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Device paired successfully"));
    }

    @PostMapping("/token/refresh")
    @Operation(summary = "Refresh device token (Called by Android)")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
            
        TokenResponse response = pairingService.refreshDeviceToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout device and invalidate tokens (Called by Android)")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestAttribute("deviceId") String deviceId) {
            
        log.info("Logging out device {}", deviceId);
        pairingService.logoutDevice(deviceId);
        return ResponseEntity.ok(ApiResponse.success(null, "Device logged out"));
    }

    @GetMapping("/devices")
    @Operation(summary = "Get all devices for the authenticated user (Called by Web App)")
    public ResponseEntity<ApiResponse<java.util.List<com.carbonfootprint.platform.mobile.model.Device>>> getDevices() {
        String webUserId = "web-user-123"; // TODO: use Spring Security
        var devices = pairingService.getDevicesForUser(webUserId);
        return ResponseEntity.ok(ApiResponse.success(devices, "Devices retrieved"));
    }

    @DeleteMapping("/devices/{deviceId}")
    @Operation(summary = "Remove a device for the authenticated user (Called by Web App)")
    public ResponseEntity<ApiResponse<Void>> removeDevice(@PathVariable String deviceId) {
        String webUserId = "web-user-123"; // TODO: use Spring Security
        pairingService.removeDeviceForUser(deviceId, webUserId);
        return ResponseEntity.ok(ApiResponse.success(null, "Device removed"));
    }
}
