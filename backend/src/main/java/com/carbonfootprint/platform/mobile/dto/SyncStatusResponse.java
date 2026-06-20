package com.carbonfootprint.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncStatusResponse {

    private String deviceId;
    private Instant lastSuccessfulSync;
    private int pendingTransactionsCount;
    private boolean isSyncInProgress;
}
