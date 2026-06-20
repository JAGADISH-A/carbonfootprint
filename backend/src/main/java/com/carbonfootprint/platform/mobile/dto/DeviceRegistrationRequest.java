package com.carbonfootprint.platform.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {

    @NotBlank(message = "Pairing code cannot be blank")
    private String pairingCode;

    @NotBlank(message = "Device ID cannot be blank")
    private String deviceId;

    @NotBlank(message = "Device name cannot be blank")
    private String deviceName;

    @NotBlank(message = "Manufacturer cannot be blank")
    private String manufacturer;

    @NotBlank(message = "Model cannot be blank")
    private String model;

    @NotBlank(message = "Android version cannot be blank")
    private String androidVersion;

    @NotBlank(message = "App version cannot be blank")
    private String appVersion;

    @NotNull(message = "Notification permission status is required")
    private Boolean notificationPermission;

    @NotNull(message = "SMS permission status is required")
    private Boolean smsPermission;

    @NotNull(message = "Sync enabled status is required")
    private Boolean syncEnabled;
}
