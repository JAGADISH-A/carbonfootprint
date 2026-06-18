package com.carbonfootprint.platform.carbon.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Monthly emission data point for trend display.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyEmissionTrend {

    private String month;

    private BigDecimal carbonKg;

    private int activityCount;
}
