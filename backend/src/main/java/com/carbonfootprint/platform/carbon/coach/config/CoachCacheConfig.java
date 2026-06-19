package com.carbonfootprint.platform.carbon.coach.config;

import com.carbonfootprint.platform.carbon.coach.service.CoachCacheService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the AI Coach response cache.
 *
 * <p>Registers {@link CoachCacheService} as a Spring bean,
 * injected with {@link CoachCacheProperties} for TTL and enablement.</p>
 */
@Configuration
@EnableConfigurationProperties(CoachCacheProperties.class)
public class CoachCacheConfig {

    @Bean
    public CoachCacheService coachCacheService(CoachCacheProperties properties) {
        return new CoachCacheService(properties);
    }
}
