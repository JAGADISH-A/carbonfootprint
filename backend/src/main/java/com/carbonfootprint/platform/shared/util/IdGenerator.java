package com.carbonfootprint.platform.shared.util;

import java.util.UUID;

/**
 * Centralised factory for generating unique identifiers.
 *
 * <p>All domain entity IDs in the platform must be generated through this class
 * to ensure consistency. The underlying strategy can be swapped (e.g., from
 * UUID to ULID) without touching any domain model.
 */
public final class IdGenerator {

    private IdGenerator() {
        // Utility class — no instantiation
    }

    /**
     * Generates a new random UUID v4 string.
     *
     * @return lowercase UUID string, e.g. "550e8400-e29b-41d4-a716-446655440000"
     */
    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
