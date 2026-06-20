package com.carbonfootprint.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileSyncResponse {

    private String syncId;
    private int processedCount;
    private int successCount;
    private int failureCount;
    private List<String> failedTransactionIds;
}
