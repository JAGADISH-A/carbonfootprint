package com.carbonfootprint.platform.carbon.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Emission total for a single {@code ActivityCategory}.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryEmissionSummary {

    private String category;

    private BigDecimal carbonKg;

    private int activityCount;

    private BigDecimal percentageOfTotal;
}
