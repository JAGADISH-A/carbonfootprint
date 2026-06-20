package com.carbonfootprint.platform.mobile.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenResponse {
    private String deviceToken;
    private String refreshToken;
    private long expiresInSeconds;
}
