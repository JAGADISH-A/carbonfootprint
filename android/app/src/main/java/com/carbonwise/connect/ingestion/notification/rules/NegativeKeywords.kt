package com.carbonwise.connect.ingestion.notification.rules

/**
 * Centralized list of negative keywords that decrease relevance scoring.
 *
 * Notifications whose title or body contains one or more of these keywords
 * receive a negative signal during the relevance filter. The more keywords
 * matched, the lower the confidence score.
 *
 * # Adding a new negative keyword
 *
 * 1. Add the keyword (lowercase) to [values].
 * 2. Use the most specific form; prefer "game update" over "game".
 * 3. Place it under the correct category comment for traceability.
 *
 * # Notes
 *
 * - Keywords are lowercase and matched case-insensitively.
 * - Exact substring matching is used; "promo" matches "promotional" and "promos".
 * - Multi-word phrases are supported; match them as complete phrases.
 */
object NegativeKeywords {

    /**
     * Immutable set of negative keywords.
     *
     * Grouped by category. When adding new keywords, append to the appropriate group
     * and maintain alphabetical order within the group.
     */
    val values: Set<String> = setOf(
        // Promotional / marketing
        "promo",
        "promotion",
        "promotional",
        "sale",
        "discount",
        "coupon",
        "deal of the day",
        "limited time offer",
        "flash sale",
        "black friday",
        "cyber monday",
        "percent off",
        "% off",

        // Notifications / alerts
        "notification",
        "alert",
        "reminder",
        "update available",
        "new message",
        "you have a new",
        "someone liked",
        "commented on",
        "shared with you",
        "tagged you",

        // Social / engagement
        "friend request",
        "new follower",
        "people you may know",
        "suggested for you",
        "trending",
        "viral",
        "popular near you",

        // Gaming
        "game update",
        "new season",
        "battle pass",
        "daily reward",
        "energy refill",
        "level up",
        "unlock",

        // Spam / junk
        "unsubscribe",
        "opt out",
        "view in browser",
        "email preferences",
        "manage subscriptions",
    )
}
