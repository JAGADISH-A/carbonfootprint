package com.carbonfootprint.platform.platform.config;

import com.carbonfootprint.platform.shared.constant.ApiConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration.
 *
 * <h3>Current phase</h3>
 * Sets up the security skeleton: stateless session, CSRF disabled (REST API),
 * and public access for the health endpoint. All other endpoints return 401
 * until authentication is wired.
 *
 * <h3>OAuth 2.0 Resource Server (Phase 2)</h3>
 * To enable Google ID token validation, add:
 * <pre>
 * .oauth2ResourceServer(oauth2 -> oauth2
 *     .jwt(jwt -> jwt
 *         .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")))
 * </pre>
 * And configure the issuer in application.yml:
 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri=https://accounts.google.com}
 *
 * <h3>CORS</h3>
 * Cross-origin requests are handled by {@link WebConfig}.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            ApiConstants.HEALTH_PATH,
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/actuator/health"
    };

    private final com.carbonfootprint.platform.mobile.security.DeviceTokenFilter deviceTokenFilter;

    public SecurityConfig(com.carbonfootprint.platform.mobile.security.DeviceTokenFilter deviceTokenFilter) {
        this.deviceTokenFilter = deviceTokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF — REST API with JWT tokens; CSRF not needed
                .csrf(csrf -> csrf.disable())

                // Stateless — no HTTP session; JWT carries authentication
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Authorization rules
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        // Phase 4.4 specific: Mobile pairing endpoints are public
                        .requestMatchers("/api/v1/mobile/pair", "/api/v1/mobile/token/refresh", "/api/v1/mobile/pairing/generate").permitAll()
                        // TODO (Phase 2): Uncomment when OAuth2 Resource Server is configured
                        // .anyRequest().authenticated()
                        .anyRequest().permitAll() // Temporarily open — remove in Phase 2
                )
                
                // Add DeviceTokenFilter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(deviceTokenFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        // TODO (Phase 2): Enable JWT validation
        // .oauth2ResourceServer(oauth2 -> oauth2
        //     .jwt(jwt -> jwt
        //         .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")));

        return http.build();
    }
}
