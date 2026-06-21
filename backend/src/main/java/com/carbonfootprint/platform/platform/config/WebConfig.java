package com.carbonfootprint.platform.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Web MVC configuration including CORS settings.
 *
 * <h3>CORS</h3>
 * Allowed origins are configured via {@code carbon.cors.allowed-origins}
 * (never hardcoded). Defaults to localhost for local development.
 *
 * <h3>File upload size</h3>
 * Max upload size is configured via:
 * {@code spring.servlet.multipart.max-file-size}
 * {@code spring.servlet.multipart.max-request-size}
 * (see application.yml)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${carbon.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private List<String> allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-Device-Token", "X-Requested-With"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // 1 hour preflight cache

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }
}
