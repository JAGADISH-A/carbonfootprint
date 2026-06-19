package com.carbonfootprint.platform.carbon.coach.service;

/**
 * Immutable snapshot of cache runtime statistics.
 *
 * <p>Returned by {@link CoachCacheService#getStatistics()}.
 * All values are counters captured at a point in time — they are
 * monotonically increasing (never reset) unless the service is restarted.</p>
 *
 * @param hits               number of cache hits (valid entry returned)
 * @param misses             number of cache misses (no entry or expired)
 * @param stores             number of successful AI responses stored
 * @param expiredRemovals    number of expired entries lazily removed
 * @param currentSize        current number of entries in the cache
 * @param hitRatio           hits / (hits + misses), or 0.0 if no operations
 * @param missRatio          misses / (hits + misses), or 0.0 if no operations
 * @param groqCallsAvoided   equivalent to hits (each hit prevents a Groq call)
 */
public record CoachCacheStatistics(
        long hits,
        long misses,
        long stores,
        long expiredRemovals,
        int currentSize,
        double hitRatio,
        double missRatio,
        long groqCallsAvoided
) {
    /**
     * Returns a formatted summary for logging.
     */
    public String toSummaryString() {
        return String.format(
                "Cache Stats — Hit Rate: %.1f%% | Miss Rate: %.1f%% | Entries: %d | Groq Calls Prevented: %d",
                hitRatio * 100, missRatio * 100, currentSize, groqCallsAvoided);
    }
}
