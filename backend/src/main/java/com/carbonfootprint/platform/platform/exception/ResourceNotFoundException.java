package com.carbonfootprint.platform.platform.exception;

/**
 * Thrown when a domain entity cannot be found by the given identifier.
 *
 * <p>Mapped to HTTP 404 by {@link com.carbonfootprint.platform.platform.web.GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends CarbonPlatformException {

    public ResourceNotFoundException(String resourceType, String id) {
        super(resourceType + " not found with id='" + id + "'.");
    }
}
