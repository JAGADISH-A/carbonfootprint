package com.carbonfootprint.platform.mobile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobileSyncRequest {

    @NotBlank(message = "Device ID cannot be blank")
    private String deviceId;

    @NotBlank(message = "Sync ID cannot be blank")
    private String syncId;

    @NotNull(message = "Transactions list cannot be null")
    @Valid
    private List<EnrichedTransaction> transactions;
}
