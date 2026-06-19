package com.carbonfootprint.platform.integration.ai.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Type-safe configuration for AI provider selection.
 *
 * <h3>Usage</h3> {@code ai.provider=groq}
 *
 * <h3>Default</h3> {@code groq}
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * Active AI document-parsing provider.
     * Accepted values: {@code groq}.
     */
    private String provider = "groq";
}
