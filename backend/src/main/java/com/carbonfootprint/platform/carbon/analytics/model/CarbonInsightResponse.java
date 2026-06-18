package com.carbonfootprint.platform.carbon.analytics.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO for deterministic carbon emission insights.
 *
 * <h3>Fields</h3>
 * <ul>
 *   <li><strong>summary</strong> — one-paragraph human-readable overview</li>
 *   <li><strong>achievements</strong> — positive observations (e.g., low-emission month)</li>
 *   <li><strong>warnings</strong> — concerning patterns (e.g., high-emission category)</li>
 *   <li><strong>recommendations</strong> — actionable suggestions to reduce emissions</li>
 *   <li><strong>insights</strong> — general factual observations about the data</li>
 * </ul>
 *
 * <h3>Design</h3>
 * All lists are non-null. Empty list means no items of that type.
 * The response is fully deterministic — no external service calls.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CarbonInsightResponse {

    private String summary;

    private List<String> achievements;

    private List<String> warnings;

    private List<String> recommendations;

    private List<String> insights;
}
