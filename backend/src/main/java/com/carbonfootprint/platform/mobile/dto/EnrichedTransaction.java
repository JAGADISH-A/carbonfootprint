package com.carbonfootprint.platform.mobile.dto;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Data Transfer Object representing a pre-enriched transaction sent by the Android Companion App.
 *
 * <p>This DTO contains core transaction details and optional carbon hints that
 * were extracted on-device via local ML models or rule engines.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnrichedTransaction {

    @NotBlank(message = "Transaction ID cannot be blank")
    private String transactionId;

    @NotBlank(message = "Merchant cannot be blank")
    private String merchant;

    @NotNull(message = "Amount is required")
    @PositiveOrZero(message = "Amount cannot be negative")
    private BigDecimal amount;

    @NotBlank(message = "Currency cannot be blank")
    private String currency;

    @NotNull(message = "Category is required")
    private ActivityCategory category;

    private String unit;
    private String location;

    @NotNull(message = "occurredAt timestamp is required")
    private Instant occurredAt;

    private String description;
    
    /**
     * Pre-resolved hints such as activityType, transportMode, fuelType, etc.
     */
    @Builder.Default
    private Map<String, Object> carbonHints = new HashMap<>();
    
    /**
     * Additional flexible attributes.
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
