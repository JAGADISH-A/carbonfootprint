package com.carbonfootprint.platform.mobile.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PairingCodeResponse {
    private String pairingCode;
    private Instant expiresAt;
    private int expiresInSeconds;
}
