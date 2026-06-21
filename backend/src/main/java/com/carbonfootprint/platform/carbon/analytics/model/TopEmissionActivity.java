package com.carbonfootprint.platform.carbon.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Summary of a single high-emission activity for the top-5 list.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopEmissionActivity {

    private String activityId;

    private String category;

    private String merchant;

    private BigDecimal carbonKg;

    private String methodology;

    private Instant occurredAt;

    private Double confidence;
}

