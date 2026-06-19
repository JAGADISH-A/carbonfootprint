package com.carbonwise.connect.ingestion.notification.rules

/**
 * Centralized list of positive keywords that increase relevance scoring.
 *
 * Notifications whose title or body contains one or more of these keywords
 * receive a positive signal during the relevance filter. The more keywords
 * matched, the higher the confidence score.
 *
 * # Adding a new positive keyword
 *
 * 1. Add the keyword (lowercase) to [values].
 * 2. Use the most specific form; prefer "ride completed" over "ride".
 * 3. Place it under the correct category comment for traceability.
 *
 * # Notes
 *
 * - Keywords are lowercase and matched case-insensitively.
 * - Exact substring matching is used; "order" matches "orders" and "ordered".
 * - Multi-word phrases are supported; match them as complete phrases.
 */
object PositiveKeywords {

    /**
     * Immutable set of positive keywords.
     *
     * Grouped by category. When adding new keywords, append to the appropriate group
     * and maintain alphabetical order within the group.
     */
    val values: Set<String> = setOf(
        // Ride-hailing / transport
        "ride completed",
        "trip completed",
        "trip finished",
        "driver arriving",
        "driver is on the way",
        "pickup confirmed",
        "ride confirmed",
        "eta",
        "drop-off",
        "carpool",
        "ride share",

        // Delivery / shipping
        "out for delivery",
        "delivered",
        "delivery confirmed",
        "on the way",
        "arriving soon",
        "package left at",
        "tracking number",
        "in transit",

        // Food delivery
        "order confirmed",
        "food is on the way",
        "dasher is on the way",
        "courier is on the way",
        "your order has been delivered",
        "restaurant confirmed",
        "preparing your order",

        // Grocery
        "groceries delivered",
        "shopping list",
        "order picked up",

        // Purchase / transaction
        "purchase confirmed",
        "payment confirmed",
        "order placed",
        "receipt",
        "transaction",
        "total charged",
        "order total",

        // Energy / utilities
        "energy bill",
        "electric bill",
        "utility bill",
        "meter reading",
        "usage report",

        // Travel
        "booking confirmed",
        "reservation confirmed",
        "flight booked",
        "check-in open",
        "boarding pass",
    )
}
