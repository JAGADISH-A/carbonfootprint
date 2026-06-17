package com.carbonfootprint.platform.platform.exception;

/**
 * Root exception for all platform-specific errors.
 *
 * <p>All custom exceptions in this platform extend this class, allowing
 * {@link com.carbonfootprint.platform.platform.web.GlobalExceptionHandler}
 * to handle them with a single catch clause where appropriate.
 */
public class CarbonPlatformException extends RuntimeException {

    public CarbonPlatformException(String message) {
        super(message);
    }

    public CarbonPlatformException(String message, Throwable cause) {
        super(message, cause);
    }
}
