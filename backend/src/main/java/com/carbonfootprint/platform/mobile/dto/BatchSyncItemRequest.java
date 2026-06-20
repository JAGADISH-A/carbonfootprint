package com.carbonfootprint.platform.mobile.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchSyncItemRequest {

    @NotBlank(message = "Item ID is required")
    @Schema(description = "Unique identifier of the item", example = "abc12345")
    private String id;

    @NotBlank(message = "Sender is required")
    private String sender;

    @NotBlank(message = "Message body is required")
    private String messageBody;

    private long receivedTimestamp;

    @NotBlank(message = "Normalized merchant is required")
    private String normalizedMerchant;

    private String category;

    private String source;

    private String rawHash;
    
    private int ingestionVersion;
}
