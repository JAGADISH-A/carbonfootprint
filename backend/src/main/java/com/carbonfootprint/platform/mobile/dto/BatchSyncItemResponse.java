package com.carbonfootprint.platform.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSyncItemResponse {

    @Schema(description = "Unique identifier of the item", example = "abc12345")
    private String id;

    @Schema(description = "Status of the upload: SUCCESS or FAILED", example = "SUCCESS")
    private String status;

    @Schema(description = "Reason for failure, if status is FAILED", example = "Duplicate")
    private String reason;
}
