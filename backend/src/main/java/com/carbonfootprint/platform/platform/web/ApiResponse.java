package com.carbonfootprint.platform.platform.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Generic API response envelope used by ALL endpoints.
 *
 * <h3>Contract</h3>
 * Every response from the platform wraps its payload in this object.
 * Clients can always rely on the presence of {@code success} and
 * {@code timestamp}. {@code data} is present on success;
 * {@code message} contains an error description on failure.
 *
 * <h3>Example success</h3>
 * <pre>
 * {
 *   "success": true,
 *   "data": { "id": "...", "status": "UP" },
 *   "timestamp": "2024-01-01T12:00:00Z"
 * }
 * </pre>
 *
 * <h3>Example failure</h3>
 * <pre>
 * {
 *   "success": false,
 *   "message": "Validation failed: rawText must not be blank.",
 *   "errorCode": "VALIDATION_ERROR",
 *   "timestamp": "2024-01-01T12:00:00Z"
 * }
 * </pre>
 *
 * @param <T> the type of the data payload
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;

    private T data;

    private String message;

    /** Machine-readable error code for frontend error handling. */
    private String errorCode;

    @Builder.Default
    private Instant timestamp = Instant.now();

    // ── Factory methods ────────────────────────────────────────────────────

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .build();
    }

    public static <T> ApiResponse<T> error(String message, String errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .build();
    }
}
