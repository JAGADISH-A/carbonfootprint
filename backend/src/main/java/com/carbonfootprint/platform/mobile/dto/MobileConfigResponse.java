package com.carbonfootprint.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileConfigResponse {

    private String latestAppVersion;
    private boolean forceUpdate;
    private int syncIntervalSeconds;
    private Map<String, String> featureFlags;
}
