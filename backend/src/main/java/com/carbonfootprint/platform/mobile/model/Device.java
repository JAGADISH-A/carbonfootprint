package com.carbonfootprint.platform.mobile.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Represents a registered mobile device paired to a user.
 */
@Data
@Builder
public class Device {
    private String id;
    private String userId;
    private String deviceId; // The hardware/client-provided device ID
    private String deviceName;
    private String manufacturer;
    private String model;
    private String androidVersion;
    private String appVersion;
    
    // For refresh tokens
    private String refreshTokenHash;
    private Instant refreshTokenExpiry;
    
    private Instant createdAt;
    private Instant lastSeenAt;
}
