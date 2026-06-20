package com.carbonfootprint.platform.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HeartbeatRequest {

    @NotBlank(message = "Device ID cannot be blank")
    private String deviceId;

    @NotNull(message = "Battery optimization enabled status is required")
    private Boolean batteryOptimizationEnabled;

    @NotNull(message = "lastSeen timestamp is required")
    private Instant lastSeen;

    @NotBlank(message = "App version cannot be blank")
    private String appVersion;
}
