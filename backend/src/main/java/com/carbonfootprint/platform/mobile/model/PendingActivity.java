package com.carbonfootprint.platform.mobile.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@With
public class PendingActivity {
    private String id;
    private String userId;
    private String deviceId;
    private String syncSessionId;
    private String source; // SMS or Notification
    private String rawPayload;
    
    private String merchant;
    private BigDecimal amount;
    private Instant timestamp; // receivedTimestamp
    
    @Builder.Default
    private int retryCount = 0;
    private String lastError;
    
    @Builder.Default
    private PendingActivityStatus status = PendingActivityStatus.NEW;
    
    private Instant createdAt;
    private Instant processingStartedAt;
    private Instant processedAt;
    private Instant lastRetryAt;
}
