package com.carbonfootprint.platform.carbon.coach.config;

import com.carbonfootprint.platform.carbon.coach.service.CoachCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the AI Coach response cache.
 *
 * <p>Registers {@link CoachCacheService} as a Spring bean,
 * injected with {@link CoachCacheProperties} for TTL and enablement,
 * and the application's {@link ObjectMapper} for deterministic hashing.</p>
 */
@Configuration
@EnableConfigurationProperties(CoachCacheProperties.class)
public class CoachCacheConfig {

    @Bean
    public CoachCacheService coachCacheService(CoachCacheProperties properties, ObjectMapper objectMapper) {
        return new CoachCacheService(properties, objectMapper);
    }
}
