package com.carbonfootprint.platform.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSyncResponse {

    @Schema(description = "Unique identifier for this sync session", example = "session_abcdef123")
    private String syncSessionId;

    @Schema(description = "Per-record status of the upload batch")
    private List<BatchSyncItemResponse> results;
}
