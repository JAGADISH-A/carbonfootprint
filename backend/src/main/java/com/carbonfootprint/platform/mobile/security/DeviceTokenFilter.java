package com.carbonfootprint.platform.mobile.security;

import com.carbonfootprint.platform.mobile.service.DeviceTokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceTokenFilter extends OncePerRequestFilter {

    private final DeviceTokenService deviceTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
            
        String path = request.getRequestURI();
        
        // Only protect mobile specific authenticated endpoints
        if (!path.startsWith("/api/v1/mobile/") ||
            path.equals("/api/v1/mobile/pair") ||
            path.equals("/api/v1/mobile/token/refresh") ||
            path.equals("/api/v1/mobile/pairing/generate")) {
            
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("Missing or invalid Authorization header for mobile endpoint: {}", path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid token");
            return;
        }

        String token = authHeader.substring(7);
        Optional<Claims> claimsOpt = deviceTokenService.validateToken(token);
        
        if (claimsOpt.isEmpty()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired device token");
            return;
        }
        
        Claims claims = claimsOpt.get();
        String deviceId = claims.getSubject();
        String userId = claims.get("userId", String.class);
        
        // Pass to controllers as request attributes
        request.setAttribute("deviceId", deviceId);
        request.setAttribute("userId", userId);
        
        filterChain.doFilter(request, response);
    }
}
