package com.carbonfootprint.platform.carbon.coach.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration for the AI Coach response cache.
 *
 * <h3>Usage</h3>
 * {@code
 * carbon.coach.cache.enabled=true
 * carbon.coach.cache.ttl-minutes=60
 * }
 *
 * <h3>Defaults</h3>
 * Cache enabled, 60-minute TTL.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "carbon.coach.cache")
public class CoachCacheProperties {

    /**
     * Whether the coach response cache is enabled.
     */
    private boolean enabled = true;

    /**
     * Time-to-live in minutes for cached coach responses.
     * After this duration, the entry is considered expired
     * and the next request will regenerate the response.
     */
    private int ttlMinutes = 60;
}
