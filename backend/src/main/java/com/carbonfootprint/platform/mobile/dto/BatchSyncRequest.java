package com.carbonfootprint.platform.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSyncRequest {

    @NotBlank(message = "Device ID is required")
    @Schema(description = "Unique identifier of the device performing the sync", example = "dev_123456789")
    private String deviceId;

    @NotBlank(message = "Sync Session ID is required")
    @Schema(description = "Unique identifier for this sync session", example = "session_abcdef123")
    private String syncSessionId;

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    @Schema(description = "List of pending activities to upload")
    private List<BatchSyncItemRequest> items;
}
