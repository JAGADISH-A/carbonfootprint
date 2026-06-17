package com.carbonfootprint.platform.platform.web;

import com.carbonfootprint.platform.platform.exception.CarbonPlatformException;
import com.carbonfootprint.platform.platform.exception.IngestionException;
import com.carbonfootprint.platform.platform.exception.ResourceNotFoundException;
import com.carbonfootprint.platform.platform.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.stream.Collectors;

/**
 * Global exception handler — converts all exceptions into consistent
 * {@link ApiResponse} JSON responses.
 *
 * <h3>Error Code Convention</h3>
 * <ul>
 *   <li>{@code VALIDATION_ERROR} — invalid input fields</li>
 *   <li>{@code INGESTION_ERROR} — pipeline processing failure</li>
 *   <li>{@code NOT_FOUND} — resource does not exist</li>
 *   <li>{@code PLATFORM_ERROR} — general platform exception</li>
 *   <li>{@code INTERNAL_ERROR} — unexpected server error</li>
 *   <li>{@code UPLOAD_TOO_LARGE} — file size exceeded</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── Bean Validation (@Valid) ───────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Request validation failed: {}", message);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed: " + message, "VALIDATION_ERROR"));
    }

    // ── Domain Validation (ValidationException) ───────────────────────────

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDomainValidation(ValidationException ex) {
        log.warn("Domain validation failed: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage(), "VALIDATION_ERROR"));
    }

    // ── Ingestion Pipeline ────────────────────────────────────────────────

    @ExceptionHandler(IngestionException.class)
    public ResponseEntity<ApiResponse<Void>> handleIngestionException(IngestionException ex) {
        log.error("Ingestion pipeline error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiResponse.error(ex.getMessage(), "INGESTION_ERROR"));
    }

    // ── Resource Not Found ────────────────────────────────────────────────

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage(), "NOT_FOUND"));
    }

    // ── General Platform Exception ────────────────────────────────────────

    @ExceptionHandler(CarbonPlatformException.class)
    public ResponseEntity<ApiResponse<Void>> handlePlatformException(CarbonPlatformException ex) {
        log.error("Platform error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ex.getMessage(), "PLATFORM_ERROR"));
    }

    // ── File Size Exceeded ────────────────────────────────────────────────

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleFileSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Upload size exceeded: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.error(
                        "Uploaded file exceeds the maximum allowed size.",
                        "UPLOAD_TOO_LARGE"
                ));
    }

    // ── Catch-all ─────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(
                        "An unexpected error occurred. Please try again or contact support.",
                        "INTERNAL_ERROR"
                ));
    }
}
